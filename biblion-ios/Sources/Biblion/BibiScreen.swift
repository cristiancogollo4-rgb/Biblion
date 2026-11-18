#if canImport(SwiftUI)
import SwiftUI
import BiblionCore

struct ChatMessage: Codable, Identifiable {
    var id = UUID()
    var question: String
    var answer: String
    var term: String?
    var local = true
}
struct ChatSession: Codable, Identifiable {
    var id = UUID()
    var title = "Nueva conversación"
    var messages: [ChatMessage] = []
}
@MainActor
final class ChatStore: ObservableObject {
    @Published var sessions: [ChatSession] = []
    @Published var error: String?
    private let url: URL?
    private var canWrite = true
    init() {
        url = try? ReadingStateStore.applicationURL().deletingLastPathComponent().appendingPathComponent("bibi-sessions.json")
        guard let url, FileManager.default.fileExists(atPath: url.path) else { return }
        do { sessions = try JSONDecoder().decode([ChatSession].self, from: Data(contentsOf: url)) }
        catch { canWrite = false; self.error = "No pudimos abrir tu historial. Conservamos el archivo original." }
    }
    func save() {
        guard canWrite else { return }
        guard let url else { error = "No pudimos acceder al historial local."; return }
        do {
            try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
            try JSONEncoder().encode(sessions).write(to: url, options: .atomic)
        } catch { self.error = "No pudimos guardar la conversación." }
    }
}
struct BibiScreen: View {
    @EnvironmentObject private var app: AppModel
    @EnvironmentObject private var cloud: CloudAccount
    @EnvironmentObject private var chats: ChatStore
    let location: ReadingLocation?
    var initialQuestion = ""
    @State private var sessionID: UUID?
    @State private var question = ""
    @State private var sending = false
    @State private var failure: String?
    @State private var showHistory = false
    @State private var rename = ""
    @State private var renaming: UUID?
    private var current: ChatSession? { chats.sessions.first { $0.id == sessionID } }
    var body: some View {
        VStack(spacing: 0) {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 20) {
                        VStack(alignment: .leading, spacing: 10) {
                            Label("Bibi", systemImage: "bubble.left.and.bubble.right.fill").font(.title2.bold()).foregroundStyle(BiblionTheme.blue)
                            Text("¡Hola\(greetingName)! Soy Bibi. Puedo ayudarte a explorar personajes, lugares y temas de la Biblia\(location.map { ". Estás leyendo \($0.reference)" } ?? "").")
                            Text("Las consultas locales funcionan sin conexión. Al profundizar, tu pregunta y el contexto bíblico se envían al servicio de Bibi.")
                                .font(.caption).foregroundStyle(BiblionTheme.secondary)
                        }
                        ForEach(current?.messages ?? []) { message in
                            VStack(alignment: .leading, spacing: 12) {
                                Text(message.question).font(.headline).frame(maxWidth: .infinity, alignment: .trailing)
                                    .padding(14).background(BiblionTheme.highlight, in: RoundedRectangle(cornerRadius: 14))
                                Text(message.answer).textSelection(.enabled)
                                Label(message.local ? "Biblioteca local" : "Bibi · En línea", systemImage: message.local ? "books.vertical" : "network")
                                    .font(.caption).foregroundStyle(BiblionTheme.secondary)
                                if let term = message.term {
                                    Button("Profundizar sobre \(term)") { question = "Profundiza sobre \(term)"; send() }.buttonStyle(.bordered).foregroundStyle(BiblionTheme.blue).disabled(sending)
                                }
                            }.id(message.id)
                        }
                        if sending { ProgressView("Bibi está consultando…") }
                        if let failure { Text(failure).foregroundStyle(.red); Button("Reintentar") { send() } }
                        if let error = chats.error { Text(error).foregroundStyle(.red) }
                        Color.clear.frame(height: 1).id("bottom")
                    }.padding(20).frame(maxWidth: 760).frame(maxWidth: .infinity)
                }
                .onChange(of: current?.messages.count) { _, _ in withAnimation { proxy.scrollTo("bottom", anchor: .bottom) } }
            }
            HStack(alignment: .bottom, spacing: 12) {
                TextField("Pregunta sobre la Biblia", text: $question, axis: .vertical).lineLimit(1...5).textFieldStyle(.roundedBorder)
                Button { send() } label: { Image(systemName: "arrow.up.circle.fill").font(.title).frame(minWidth: 44, minHeight: 44) }
                    .disabled(sending || question.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty).accessibilityLabel("Enviar pregunta")
            }.padding().background(BiblionTheme.surface)
        }.navigationTitle("Bibi").navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) { Button { showHistory = true } label: { Image(systemName: "clock").frame(minWidth: 44, minHeight: 44) }.accessibilityLabel("Conversaciones") }
            ToolbarItem(placement: .topBarTrailing) { Button { sessionID = nil; failure = nil } label: { Image(systemName: "square.and.pencil").frame(minWidth: 44, minHeight: 44) }.accessibilityLabel("Nueva conversación") }
        }
        .sheet(isPresented: $showHistory) {
            NavigationStack {
                List {
                    ForEach(chats.sessions) { session in
                        Button(session.title) { sessionID = session.id; showHistory = false }
                            .swipeActions { Button("Eliminar", role: .destructive) { chats.sessions.removeAll { $0.id == session.id }; chats.save() } }
                            .contextMenu { Button("Renombrar") { rename = session.title; renaming = session.id; showHistory = false } }
                    }
                    if chats.sessions.isEmpty { Text("Tus conversaciones aparecerán aquí.") }
                }.navigationTitle("Conversaciones").toolbar { Button("Cerrar") { showHistory = false } }
            }
        }
        .alert("Renombrar conversación", isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
            TextField("Título", text: $rename)
            Button("Guardar") { if let i = chats.sessions.firstIndex(where: { $0.id == renaming }), !rename.trimmingCharacters(in: .whitespaces).isEmpty { chats.sessions[i].title = String(rename.prefix(80)); chats.save() }; renaming = nil }
            Button("Cancelar", role: .cancel) { renaming = nil }
        }
        .task { if !initialQuestion.isEmpty && current == nil { question = initialQuestion } }
        .biblionPage()
    }
    private var greetingName: String {
        let name = cloud.profile["nombres"] ?? ""
        return name.isEmpty ? "" : " \(name)"
    }
    private func send() {
        let prompt = String(question.trimmingCharacters(in: .whitespacesAndNewlines).prefix(1200))
        guard !prompt.isEmpty, !sending else { return }
        sending = true; failure = nil
        if current == nil { let session = ChatSession(title: String(prompt.prefix(60))); chats.sessions.insert(session, at: 0); sessionID = session.id }
        let targetID = sessionID
        let history = current?.messages ?? []
        Task {
            defer { sending = false }
            do {
                let message = try await answer(prompt, history: history)
                guard let index = chats.sessions.firstIndex(where: { $0.id == targetID }) else { return }
                chats.sessions[index].messages.append(message); chats.save(); question = ""
            } catch { failure = error.localizedDescription }
        }
    }
    private func answer(_ prompt: String, history: [ChatMessage]) async throws -> ChatMessage {
        guard let library = app.library else { throw BibleError.missingDatabase }
        let key = prompt.searchKey.trimmingCharacters(in: .punctuationCharacters.union(.whitespaces))
        if ["hola", "buenas", "buenos dias", "buenas tardes"].contains(key) { return ChatMessage(question: prompt, answer: "¡Hola! Podemos explorar un personaje, una palabra, un lugar o el pasaje que estás leyendo.") }
        let deeper = ["profundiza", "amplia", "explica mas", "compara", "diferencia"].contains { key.contains($0) }
        let pattern = #"\b[HG]\d{1,5}\b"#
        if let range = prompt.uppercased().range(of: pattern, options: .regularExpression), let entry = try await library.strong(String(prompt.uppercased()[range])) {
            return ChatMessage(question: prompt, answer: ([entry.title, entry.definition] + entry.metadata).joined(separator: "\n\n"), term: entry.title)
        }
        if key.contains("compara") && key.contains("version"), let location, let db = app.database {
            var answers: [String] = []
            for version in app.versions {
                var target = location; target.version = version.id
                if let chapter = try? await db.chapter(at: target), let verse = chapter.verses.first(where: { $0.number == (location.verse ?? 1) }) {
                    answers.append("\(version.abbreviation)\n\(verse.text)")
                } else { answers.append("\(version.abbreviation): texto no disponible en Biblion.") }
            }
            return ChatMessage(question: prompt, answer: "\(location.reference)\n\n" + answers.joined(separator: "\n\n"))
        }
        if !deeper {
            var term = key
            for prefix in ["quien fue ", "quien es ", "quien era ", "donde esta ", "donde queda ", "donde vivio ", "donde nacio ", "que significa ", "que es ", "define ", "hablame de ", "cuentame de ", "dime sobre "] {
                if term.hasPrefix(prefix) { term = String(term.dropFirst(prefix.count)); break }
            }
            term = term.trimmingCharacters(in: .punctuationCharacters.union(.whitespaces))
            if term.count < 3 || ["y que mas", "y el", "y ella", "donde vivio", "donde nacio"].contains(term) { term = history.last(where: { $0.term != nil })?.term ?? term }
            if let entry = try await library.exact(term) {
                return ChatMessage(question: prompt, answer: ([entry.title, entry.definition] + entry.metadata).joined(separator: "\n\n"), term: entry.title)
            }
            if key.contains("relacionad"), let location {
                let refs = try await library.crossReferences(location)
                return ChatMessage(question: prompt, answer: refs.isEmpty ? "No encontré referencias relacionadas para este versículo en la biblioteca local." : "Referencias relacionadas con \(location.reference):\n\n" + refs.joined(separator: "\n"))
            }
            if key.contains("versiculos sobre") || key.contains("pasajes sobre") {
                let term = key.components(separatedBy: "sobre").last?.trimmingCharacters(in: .whitespaces) ?? ""
                if let topic = try await library.entries(query: term, topics: true).first {
                    let refs = try await library.topicReferences(topic.id)
                    return ChatMessage(question: prompt, answer: topic.title + "\n\n" + topic.definition + "\n\n" + refs.prefix(10).map(\.label).joined(separator: "\n"), term: topic.title)
                }
            }
        }
        // Unrelated subject matter never needs a network request.
        if ["programacion", "codigo python", "futbol", "receta", "politica", "bitcoin", "clima"].contains(where: { key.contains($0) }) {
            return ChatMessage(question: prompt, answer: "Estoy diseñada para ayudarte únicamente con temas bíblicos dentro de Biblion. ¿Te gustaría explorar algún pasaje, personaje o tema?")
        }
        if !deeper {
            let candidates = try await library.entries(query: key.trimmingCharacters(in: .punctuationCharacters))
            if !candidates.isEmpty { return ChatMessage(question: prompt, answer: "Encontré estas entradas: " + candidates.prefix(5).map(\.title).joined(separator: ", ") + ". ¿Cuál quieres consultar?") }
        }
        let token = try await cloud.bearer()
        var passages: [String] = []
        let independent = ["quien", "donde", "significa", "que es", "define", "hebreo", "griego"].contains { key.hasPrefix($0) }
        if !independent, let location, let db = app.database {
            let chapter = try await db.chapter(at: location)
            passages = chapter.verses.filter { location.verse == nil || $0.number == location.verse }.prefix(12).map { "\(BibleBook.displayName(for: location.book)) \(location.chapter):\($0.number) (\(location.version)): \($0.text)" }
        }
        let payload: [String: Any] = ["question": prompt, "mode": "reader", "intent": "question",
            "study": ["title": independent ? "" : location?.reference ?? "", "selectedText": passages.joined(separator: "\n")],
            "bible": ["version": location?.version ?? app.state.selectedVersion, "availableVersions": app.versions.map(\.id), "passages": passages],
            "chatHistory": history.suffix(5).map { ["question": $0.question, "response": String($0.answer.prefix(1800)), "resolvedTerm": $0.term ?? ""] }]
        let result = try await cloud.request(URL(string: cloud.config["bibiURL"]!)!, method: "POST", body: payload, token: token)
        guard let rawAnswer = result["answer"] as? String, let answer = AssistantAnswer.clean(rawAnswer) else { throw ServiceFailure(message: "Bibi no pudo responder. Inténtalo de nuevo.") }
        return ChatMessage(question: prompt, answer: answer, local: false)
    }
}
#endif
