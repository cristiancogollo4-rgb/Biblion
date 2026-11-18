#if canImport(SwiftUI)
import Foundation
import SwiftUI
import BiblionCore
import Security
import AuthenticationServices
import CryptoKit
import UIKit

struct ServiceFailure: LocalizedError {
    let message: String
    var errorDescription: String? { message }
}
struct AccountSession: Codable {
    var uid: String
    var email: String
    var token: String
    var refresh: String
    var expires: Date
}

@MainActor
final class CloudAccount: NSObject, ObservableObject, ASWebAuthenticationPresentationContextProviding {
    @Published private(set) var session: AccountSession?
    @Published private(set) var profile: [String: String] = [:]
    @Published var busy = false
    @Published var message: String?
    let config: [String: String]
    private var webSession: ASWebAuthenticationSession?
    private var syncInProgress = false
    override init() {
        if let url = Bundle.module.url(forResource: "Services", withExtension: "plist"), let data = try? Data(contentsOf: url) {
            config = (try? PropertyListSerialization.propertyList(from: data, format: nil) as? [String: String]) ?? [:]
        } else { config = [:] }
        super.init()
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: "com.cristiancogollo.biblion.ios.account", kSecReturnData as String: true]
        var item: CFTypeRef?
        if SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess, let data = item as? Data { session = try? JSONDecoder().decode(AccountSession.self, from: data) }
    }

    var googleConfigured: Bool { config["googleIOSClientID"] != nil && config["googleRedirectScheme"] != nil }
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.flatMap(\.windows).first(where: \.isKeyWindow) ?? ASPresentationAnchor()
    }
    func googleSignIn() async throws {
        guard let clientID = config["googleIOSClientID"], let scheme = config["googleRedirectScheme"] else { throw ServiceFailure(message: "Falta registrar Biblion iOS en Firebase y añadir GoogleService-Info.plist.") }
        func random() throws -> String {
            var bytes = [UInt8](repeating: 0, count: 32)
            guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else { throw ServiceFailure(message: "No pudimos preparar el acceso seguro.") }
            return Data(bytes).base64EncodedString().replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
        }
        let verifier = try random(); let state = try random()
        let challenge = Data(SHA256.hash(data: Data(verifier.utf8))).base64EncodedString().replacingOccurrences(of: "+", with: "-").replacingOccurrences(of: "/", with: "_").replacingOccurrences(of: "=", with: "")
        let redirect = scheme + ":/oauthredirect"
        var url = URLComponents(string: "https://accounts.google.com/o/oauth2/v2/auth")!
        url.queryItems = ["client_id": clientID, "redirect_uri": redirect, "response_type": "code", "scope": "openid email profile", "state": state, "code_challenge": challenge, "code_challenge_method": "S256"].map { URLQueryItem(name: $0.key, value: $0.value) }
        let callback: URL = try await withCheckedThrowingContinuation { continuation in
            let auth = ASWebAuthenticationSession(url: url.url!, callbackURLScheme: scheme) { callback, error in
                if let callback { continuation.resume(returning: callback) }
                else { continuation.resume(throwing: ServiceFailure(message: "Se canceló el acceso con Google.")) }
            }
            auth.presentationContextProvider = self; webSession = auth
            if !auth.start() { continuation.resume(throwing: ServiceFailure(message: "No pudimos abrir el acceso con Google.")) }
        }
        webSession = nil
        let params = URLComponents(url: callback, resolvingAgainstBaseURL: false)?.queryItems ?? []
        guard params.first(where: { $0.name == "state" })?.value == state, let code = params.first(where: { $0.name == "code" })?.value else { throw ServiceFailure(message: "Google no devolvió una autorización válida.") }
        var form = URLComponents(); form.queryItems = ["client_id": clientID, "code": code, "code_verifier": verifier, "grant_type": "authorization_code", "redirect_uri": redirect].map { URLQueryItem(name: $0.key, value: $0.value) }
        var request = URLRequest(url: URL(string: "https://oauth2.googleapis.com/token")!); request.httpMethod = "POST"; request.timeoutInterval = 30
        request.httpBody = Data((form.percentEncodedQuery ?? "").replacingOccurrences(of: "+", with: "%2B").utf8); request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        let (data,response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200, let result = try JSONSerialization.jsonObject(with: data) as? [String: Any], let googleToken = result["id_token"] as? String else { throw ServiceFailure(message: "No pudimos verificar la cuenta de Google.") }
        var post = URLComponents(); post.queryItems = [URLQueryItem(name: "id_token", value: googleToken), URLQueryItem(name: "providerId", value: "google.com")]
        let firebase = try await self.request(URL(string: "https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=\(config["apiKey"] ?? "")")!, method: "POST", body: ["postBody": post.percentEncodedQuery ?? "", "requestUri": redirect, "returnSecureToken": true, "returnIdpCredential": true])
        guard let uid = firebase["localId"] as? String, let token = firebase["idToken"] as? String, let refresh = firebase["refreshToken"] as? String else { throw ServiceFailure(message: "No pudimos abrir tu cuenta de Biblion.") }
        session = AccountSession(uid: uid, email: firebase["email"] as? String ?? "", token: token, refresh: refresh, expires: Date().addingTimeInterval(3500))
        do { try saveSession() } catch { session = nil; throw error }
        try await loadProfile()
    }
    private func saveSession() throws {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: "com.cristiancogollo.biblion.ios.account"]
        guard let session else { SecItemDelete(query as CFDictionary); return }
        let data = try JSONEncoder().encode(session)
        let status = SecItemUpdate(query as CFDictionary, [kSecValueData as String: data] as CFDictionary)
        if status == errSecItemNotFound {
            var insert = query; insert[kSecValueData as String] = data
            insert[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            guard SecItemAdd(insert as CFDictionary, nil) == errSecSuccess else { throw ServiceFailure(message: "No pudimos guardar la sesión de forma segura.") }
        } else if status != errSecSuccess { throw ServiceFailure(message: "No pudimos actualizar la sesión segura.") }
    }
    func request(_ url: URL, method: String = "GET", body: [String: Any]? = nil, token: String? = nil) async throws -> [String: Any] {
        var request = URLRequest(url: url); request.httpMethod = method; request.timeoutInterval = 35
        if url.host?.hasSuffix("googleapis.com") == true { request.setValue("com.cristiancogollo.biblion.ios", forHTTPHeaderField: "X-Ios-Bundle-Identifier") }
        if let body { request.httpBody = try JSONSerialization.data(withJSONObject: body); request.setValue("application/json", forHTTPHeaderField: "Content-Type") }
        if let token { request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization") }
        let (data, response) = try await URLSession.shared.data(for: request)
        guard let response = response as? HTTPURLResponse else { throw ServiceFailure(message: "No recibimos respuesta del servicio.") }
        let object = (try? JSONSerialization.jsonObject(with: data) as? [String: Any]) ?? [:]
        guard (200..<300).contains(response.statusCode) else {
            let code = (object["error"] as? [String: Any])?["message"] as? String ?? ""
            let friendly: String
            switch code {
            case "INVALID_LOGIN_CREDENTIALS", "INVALID_PASSWORD", "EMAIL_NOT_FOUND": friendly = "El correo o la contraseña no son correctos."
            case "EMAIL_EXISTS": friendly = "Este correo ya tiene una cuenta. Inicia sesión."
            case "TOO_MANY_ATTEMPTS_TRY_LATER": friendly = "Demasiados intentos. Espera unos minutos."
            case "OPERATION_NOT_ALLOWED": friendly = "Este método de acceso no está habilitado en Firebase."
            default: friendly = response.statusCode == 401 ? "Inicia sesión para continuar." : response.statusCode == 403 ? "El servicio no permite esta solicitud. Revisa la configuración de acceso." : "No pudimos completar la solicitud (\(response.statusCode)). Inténtalo de nuevo."
            }
            throw ServiceFailure(message: friendly)
        }
        return object
    }
    func authenticate(email: String, password: String, register: Bool) async throws {
        let key = config["apiKey"] ?? ""
        guard !key.isEmpty else { throw ServiceFailure(message: "Falta configurar Firebase para iOS.") }
        let endpoint = register ? "signUp" : "signInWithPassword"
        let result = try await request(URL(string: "https://identitytoolkit.googleapis.com/v1/accounts:\(endpoint)?key=\(key)")!, method: "POST", body: ["email": email, "password": password, "returnSecureToken": true])
        guard let uid = result["localId"] as? String, let token = result["idToken"] as? String, let refresh = result["refreshToken"] as? String else { throw ServiceFailure(message: "No se pudo abrir la sesión.") }
        session = AccountSession(uid: uid, email: email, token: token, refresh: refresh, expires: Date().addingTimeInterval(3500))
        do { try saveSession() } catch { session = nil; throw error }
        try await loadProfile()
    }
    func bearer() async throws -> String {
        guard var current = session else { throw ServiceFailure(message: "Inicia sesión para profundizar con Bibi y sincronizar tu cuenta.") }
        if current.expires < Date() {
            var refreshRequest = URLRequest(url: URL(string: "https://securetoken.googleapis.com/v1/token?key=\(config["apiKey"] ?? "")")!)
            refreshRequest.httpMethod = "POST"; refreshRequest.timeoutInterval = 30
            refreshRequest.setValue("com.cristiancogollo.biblion.ios", forHTTPHeaderField: "X-Ios-Bundle-Identifier")
            var form = URLComponents(); form.queryItems = [URLQueryItem(name: "grant_type", value: "refresh_token"), URLQueryItem(name: "refresh_token", value: current.refresh)]
            refreshRequest.httpBody = Data((form.percentEncodedQuery ?? "").replacingOccurrences(of: "+", with: "%2B").utf8)
            refreshRequest.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
            let (data,response) = try await URLSession.shared.data(for: refreshRequest)
            guard (response as? HTTPURLResponse)?.statusCode == 200, let result = try JSONSerialization.jsonObject(with: data) as? [String: Any] else { throw ServiceFailure(message: "Tu sesión expiró. Vuelve a iniciar sesión.") }
            guard let token = result["id_token"] as? String else { throw ServiceFailure(message: "La sesión expiró. Vuelve a iniciar sesión.") }
            current.token = token; current.refresh = result["refresh_token"] as? String ?? current.refresh; current.expires = Date().addingTimeInterval(3500)
            session = current; try saveSession()
        }
        return current.token
    }
    func signOut() { session = nil; profile = [:]; try? saveSession() }
    func resetPassword(email: String) async throws {
        _ = try await request(URL(string: "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=\(config["apiKey"] ?? "")")!, method: "POST", body: ["requestType": "PASSWORD_RESET", "email": email])
    }
    private func documentURL(_ suffix: String = "") throws -> URL {
        guard let uid = session?.uid, let project = config["projectID"] else { throw ServiceFailure(message: "Inicia sesión para continuar.") }
        return URL(string: "https://firestore.googleapis.com/v1/projects/\(project)/databases/(default)/documents/users/\(uid)\(suffix)")!
    }
    func loadProfile() async throws {
        let token = try await bearer()
        // A new account has no profile yet; a missing document is valid.
        let url = try documentURL()
        var request = URLRequest(url: url); request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        let (data,response) = try await URLSession.shared.data(for: request)
        if (response as? HTTPURLResponse)?.statusCode == 404 { profile = [:]; return }
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw ServiceFailure(message: "No pudimos descargar tu perfil. Tu sesión sigue abierta.") }
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        let fields = json?["fields"] as? [String: [String: Any]] ?? [:]
        profile = fields.reduce(into: [:]) { values, field in
            if let value = field.value["stringValue"] as? String { values[field.key] = value }
            else if let value = field.value["integerValue"] as? String { values[field.key] = value }
        }
    }
    func saveProfile(_ values: [String: String]) async throws {
        let token = try await bearer()
        var parts = URLComponents(url: try documentURL(), resolvingAgainstBaseURL: false)!
        parts.queryItems = values.keys.sorted().map { URLQueryItem(name: "updateMask.fieldPaths", value: $0) }
        let fields = values.mapValues { ["stringValue": $0] }
        _ = try await request(parts.url!, method: "PATCH", body: ["fields": fields], token: token)
        profile.merge(values) { _, new in new }
    }
    func clearPhoto() async throws {
        let token = try await bearer()
        try await patchDocument("", fields: ["fotoPerfil": ["nullValue": NSNull()], "foto_perfil": ["nullValue": NSNull()]], token: token)
        profile.removeValue(forKey: "fotoPerfil")
    }
    func saveAvatar(_ color: String) async throws {
        let token = try await bearer()
        var parts = URLComponents(url: try documentURL(), resolvingAgainstBaseURL: false)!
        parts.queryItems = [URLQueryItem(name: "updateMask.fieldPaths", value: "avatarColor")]
        _ = try await request(parts.url!, method: "PATCH", body: ["fields": ["avatarColor": ["integerValue": color]]], token: token)
        profile["avatarColor"] = color
    }

    private func fetchDocument(_ suffix: String, token: String) async throws -> [String: [String: Any]] {
        var request = URLRequest(url: try documentURL(suffix)); request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        let (data,response) = try await URLSession.shared.data(for: request)
        if (response as? HTTPURLResponse)?.statusCode == 404 { return [:] }
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw ServiceFailure(message: "No pudimos leer tus datos de la nube.") }
        return (try JSONSerialization.jsonObject(with: data) as? [String: Any])?["fields"] as? [String: [String: Any]] ?? [:]
    }
    private func patchDocument(_ suffix: String, fields: [String: Any], token: String) async throws {
        var parts = URLComponents(url: try documentURL(suffix), resolvingAgainstBaseURL: false)!
        parts.queryItems = fields.keys.sorted().map { URLQueryItem(name: "updateMask.fieldPaths", value: $0) }
        _ = try await request(parts.url!, method: "PATCH", body: ["fields": fields], token: token)
    }
    func synchronize(_ app: AppModel) async throws {
        guard !syncInProgress, app.activeOwner == session?.uid, session != nil else { return }
        syncInProgress = true; defer { syncInProgress = false }
        let token = try await bearer()
        guard let uid = session?.uid else { return }
        let snapshot = app.state
        let prefs = try await fetchDocument("/preferences/app", token: token)
        guard session?.uid == uid else { return }
        func number(_ field: [String: Any]?) -> Double {
            if let n = field?["doubleValue"] as? Double { return n }
            return Double(field?["integerValue"] as? String ?? "0") ?? 0
        }
        var fields: [String: Any] = [:]
        let items = [("darkModeEnabled", "darkModeUpdatedAt"),("selectedBibleVersion", "selectedBibleVersionUpdatedAt"),("readerFontSizeSp", "readerFontSizeUpdatedAt")]
        for (name,time) in items {
            let localTime = snapshot.syncTimes?[time] ?? 0
            let remoteTime = number(prefs[time])
            if remoteTime > localTime, let value = prefs[name] {
                app.applyRemote { state in
                    if name == "darkModeEnabled", let enabled = value["booleanValue"] as? Bool { state.appearance = enabled ? .dark : .light }
                    if name == "selectedBibleVersion", let version = value["stringValue"] as? String, app.versions.contains(where: { $0.id == version }) { state.selectedVersion = version }
                    if name == "readerFontSizeSp" { state.fontSize = number(value) }
                    var times = state.syncTimes ?? [:]; times[time] = remoteTime; state.syncTimes = times
                }
            } else if localTime > remoteTime {
                fields[time] = ["integerValue": String(Int64(localTime))]
                if name == "darkModeEnabled" { fields[name] = ["booleanValue": snapshot.appearance == .dark] }
                if name == "selectedBibleVersion" { fields[name] = ["stringValue": snapshot.selectedVersion] }
                if name == "readerFontSizeSp" { fields[name] = ["integerValue": String(Int(snapshot.fontSize))] }
            }
        }
        if !fields.isEmpty { try await patchDocument("/preferences/app", fields: fields, token: token) }
        var remote: [[String: Any]] = []
        var pageToken: String?
        repeat {
            var parts = URLComponents(url: try documentURL("/chapter_highlights"), resolvingAgainstBaseURL: false)!
            parts.queryItems = [URLQueryItem(name: "pageSize", value: "300")] + (pageToken.map { [URLQueryItem(name: "pageToken", value: $0)] } ?? [])
            let result = try await request(parts.url!, token: token)
            remote += result["documents"] as? [[String: Any]] ?? []
            pageToken = result["nextPageToken"] as? String
        } while pageToken != nil
        var remoteChapters = Set<String>()
        for document in remote {
            guard session?.uid == uid else { return }
            let data = document["fields"] as? [String: [String: Any]] ?? [:]
            guard let name = data["book"]?["stringValue"] as? String, let book = BibleBook.canonicalNames.firstIndex(where: { $0.searchKey == name.searchKey }) else { continue }
            let chapter = Int(number(data["chapter"]))
            let key = "highlight:\(book):\(chapter)"
            remoteChapters.insert(key)
            let timestamp = number(data["updatedAt"])
            guard timestamp > (snapshot.syncTimes?[key] ?? 0) else { continue }
            let deleted = data["deletedAt"]?["nullValue"] == nil && data["deletedAt"] != nil
            let map = (data["verses"]?["mapValue"] as? [String: Any])?["fields"] as? [String: [String: Any]] ?? [:]
            let version = app.state.selectedVersion
            let location = ReadingLocation(version: version, book: book, chapter: chapter)
            let content = try await app.database?.chapter(at: location)
            let imported = deleted ? [] : (content?.verses ?? []).filter { map[String($0.number)] != nil }.map { verse in
                SavedVerse(location: ReadingLocation(version: version, book: book, chapter: chapter, verse: verse.number), text: verse.text, savedAt: Date(timeIntervalSince1970: timestamp / 1000), color: UInt32(truncatingIfNeeded: Int64(number(map[String(verse.number)]))))
            }
            app.applyRemote { state in
                // Preserve highlights created while the download was in progress.
                guard (state.syncTimes?[key] ?? 0) <= timestamp else { return }
                state.highlights.removeAll { $0.location.book == book && $0.location.chapter == chapter }
                state.highlights += imported
                var times = state.syncTimes ?? [:]; times[key] = timestamp; state.syncTimes = times
            }
        }
        let keys = Set((snapshot.syncTimes ?? [:]).keys.filter { $0.hasPrefix("highlight:") })
        for key in keys {
            let numbers = key.split(separator: ":")
            guard numbers.count == 3, let book = Int(numbers[1]), let chapter = Int(numbers[2]) else { continue }
            let remoteTime: Double = remote.compactMap { document -> Double? in
                let data = document["fields"] as? [String: [String: Any]] ?? [:]
                guard (data["book"]?["stringValue"] as? String)?.searchKey == BibleBook.displayName(for: book).searchKey, Int(number(data["chapter"])) == chapter else { return nil }
                return number(data["updatedAt"])
            }.max() ?? 0
            let localTime = snapshot.syncTimes?[key] ?? 0
            guard localTime > remoteTime else { continue }
            let highlights = snapshot.highlights.filter { $0.location.book == book && $0.location.chapter == chapter }
            var verses: [String: Any] = [:]
            for item in highlights { if let verse = item.location.verse { verses[String(verse)] = ["integerValue": String(Int32(bitPattern: item.color ?? 0xFFFFD54F))] } }
            let name = BibleBook.displayName(for: book)
            let documentID = name.searchKey.replacingOccurrences(of: " ", with: "_") + "__\(chapter)"
            try await patchDocument("/chapter_highlights/\(documentID)", fields: ["book": ["stringValue": name], "chapter": ["integerValue": String(chapter)], "verses": ["mapValue": ["fields": verses]], "updatedAt": ["integerValue": String(Int64(localTime))], "deletedAt": highlights.isEmpty ? ["integerValue": String(Int64(localTime))] : ["nullValue": NSNull()]], token: token)
        }
        await app.flush()
    }
    func uploadPhoto(_ data: Data) async throws {
        guard data.count <= 5 * 1024 * 1024, let uid = session?.uid, let bucket = config["storageBucket"] else { throw ServiceFailure(message: "Selecciona una imagen de menos de 5 MB.") }
        let token = try await bearer()
        let path = "profile_photos/\(uid)/avatar.jpg"
        var parts = URLComponents(string: "https://firebasestorage.googleapis.com/v0/b/\(bucket)/o")!
        parts.queryItems = [URLQueryItem(name: "uploadType", value: "media"), URLQueryItem(name: "name", value: path)]
        var request = URLRequest(url: parts.url!); request.httpMethod = "POST"; request.httpBody = data
        request.setValue("Firebase \(token)", forHTTPHeaderField: "Authorization"); request.setValue("image/jpeg", forHTTPHeaderField: "Content-Type")
        let (reply,response) = try await URLSession.shared.data(for: request)
        guard (response as? HTTPURLResponse)?.statusCode == 200, let object = try JSONSerialization.jsonObject(with: reply) as? [String: Any], let download = object["downloadTokens"] as? String else { throw ServiceFailure(message: "No pudimos subir tu foto.") }
        let escaped = path.addingPercentEncoding(withAllowedCharacters: .alphanumerics)!
        try await saveProfile(["fotoPerfil": "https://firebasestorage.googleapis.com/v0/b/\(bucket)/o/\(escaped)?alt=media&token=\(download)"])
    }
}
#endif
