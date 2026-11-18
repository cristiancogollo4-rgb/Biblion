#if canImport(SwiftUI)
import SwiftUI
import PhotosUI
import UIKit
import BiblionCore

struct ProfileScreen: View {
    @EnvironmentObject private var app: AppModel
    @EnvironmentObject private var cloud: CloudAccount
    @AppStorage("readingGuideCompleted") private var guideCompleted = false
    @State private var showAuth = false
    @State private var showEdit = false
    @State private var showGuide = false
    @State private var confirmingSignOut = false
    var body: some View {
        List {
            Section {
                HStack(spacing: 16) {
                    AvatarView(profile: cloud.profile)
                    VStack(alignment: .leading, spacing: 5) {
                        Text(displayName).font(.title2.bold())
                        Text(cloud.profile["alias"].map { "@\($0)" } ?? (cloud.session == nil ? "Tu espacio en Biblion" : cloud.session?.email ?? "")).foregroundStyle(BiblionTheme.secondary)
                    }
                }.padding(.vertical, 12)
                if let bio = cloud.profile["biografia"], !bio.isEmpty { Text(bio) }
                if cloud.session != nil {
                    Button("Actualizar perfil") { showEdit = true }
                    Button("Actualizar desde la nube") { Task { await reload() } }.disabled(cloud.busy)
                    Button("Sincronizar preferencias y resaltados") {
                        Task {
                            cloud.busy = true; defer { cloud.busy = false }
                            do { try await cloud.synchronize(app); cloud.message = "Preferencias y resaltados sincronizados." }
                            catch { cloud.message = error.localizedDescription }
                        }
                    }.disabled(cloud.busy)
                } else { Button("Iniciar sesión o crear cuenta") { showAuth = true } }
            }
            Section("Tu actividad") {
                LabeledContent("Resaltados en este dispositivo", value: String(app.state.highlights.count))
                if cloud.session != nil {
                    ForEach([("totalEnsenanzasPublicadas", "Enseñanzas publicadas"),("totalSeguidores", "Seguidores"),("totalSiguiendo", "Siguiendo")], id: \.0) { item in
                        LabeledContent(item.1, value: cloud.profile[item.0] ?? "0")
                    }
                }
                NavigationLink { HighlightsScreen() } label: { Label("Ver mis resaltados", systemImage: "bookmark") }
            }
            Section("Preferencias y ayuda") {
                NavigationLink { SettingsScreen() } label: { Label("Ajustes", systemImage: "slider.horizontal.3") }
                Button { guideCompleted = false; UserDefaults.standard.set(0, forKey: "readingGuideStep"); showGuide = true } label: { Label("Reiniciar tutorial de lectura", systemImage: "questionmark.circle") }
                NavigationLink { StudyComingSoonScreen() } label: { Label("Modo estudio · Próximamente", systemImage: "square.and.pencil") }
            }
            if let message = cloud.message { Section { Text(message).foregroundStyle(BiblionTheme.secondary) } }
            if cloud.busy { ProgressView("Actualizando…") }
            if cloud.session != nil { Section { Button("Cerrar sesión", role: .destructive) { confirmingSignOut = true } } }
        }.navigationTitle("Perfil")
        .sheet(isPresented: $showAuth) { AuthenticationScreen() }
        .sheet(isPresented: $showEdit) { EditProfileScreen() }
        .sheet(isPresented: $showGuide) { ReadingGuideScreen() }
        .confirmationDialog("¿Cerrar tu sesión de Biblion?", isPresented: $confirmingSignOut, titleVisibility: .visible) { Button("Cerrar sesión", role: .destructive) { cloud.signOut() } }
        .task { if cloud.session != nil { await reload() } }
        .biblionPage()
    }
    private var displayName: String {
        let name = [cloud.profile["nombres"], cloud.profile["apellidos"]].compactMap { $0 }.joined(separator: " ")
        return name.isEmpty ? "Lector de Biblion" : name
    }
    private func reload() async {
        cloud.busy = true; defer { cloud.busy = false }
        do { try await cloud.loadProfile(); cloud.message = nil } catch { cloud.message = error.localizedDescription }
    }
}
struct AvatarView: View {
    let profile: [String: String]
    private var color: Color {
        let value = UInt64(profile["avatarColor"] ?? "4278939295") ?? 4278939295
        return Color(red: Double((value >> 16) & 255) / 255, green: Double((value >> 8) & 255) / 255, blue: Double(value & 255) / 255)
    }
    var body: some View {
        Group {
            if let photo = profile["fotoPerfil"], let url = URL(string: photo), url.scheme == "https" {
                AsyncImage(url: url) { image in image.resizable().scaledToFill() } placeholder: { initials }
            } else { initials }
        }.frame(width: 72, height: 72).clipShape(Circle()).accessibilityLabel("Foto de perfil")
    }
    private var initials: some View {
        Text(String((profile["nombres"] ?? "B").prefix(1)).uppercased()).font(.largeTitle.bold()).foregroundStyle(.white).frame(maxWidth: .infinity, maxHeight: .infinity).background(color)
    }
}
struct AuthenticationScreen: View {
    @EnvironmentObject private var cloud: CloudAccount
    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    @State private var password = ""
    @State private var register = false
    @State private var busy = false
    @State private var error: String?
    @State private var notice: String?
    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Acceso", selection: $register) { Text("Iniciar sesión").tag(false); Text("Crear cuenta").tag(true) }.pickerStyle(.segmented)
                    TextField("Correo electrónico", text: $email).keyboardType(.emailAddress).textContentType(.username).textInputAutocapitalization(.never).autocorrectionDisabled()
                    SecureField("Contraseña", text: $password).textContentType(register ? .newPassword : .password)
                    if register { Text("Usa al menos 6 caracteres.").font(.caption) }
                    if let error { Text(error).foregroundStyle(.red) }
                    if let notice { Text(notice).foregroundStyle(BiblionTheme.blue) }
                    if busy { ProgressView("Conectando…") }
                    Button(register ? "Crear cuenta" : "Iniciar sesión") { authenticate() }.disabled(busy || email.isEmpty || password.count < 6)
                    Button("¿Olvidaste tu contraseña?") {
                        busy = true; error = nil
                        Task { defer { busy = false }; do { try await cloud.resetPassword(email: email.trimmingCharacters(in: .whitespaces)); notice = "Si el correo está registrado, recibirás las instrucciones." } catch { self.error = error.localizedDescription } }
                    }.disabled(busy || email.isEmpty)
                }
                Section("Google") {
                    Button("Continuar con Google") {
                        busy = true; error = nil
                        Task { defer { busy = false }; do { try await cloud.googleSignIn(); dismiss() } catch { self.error = error.localizedDescription } }
                    }.disabled(busy || !cloud.googleConfigured)
                    if !cloud.googleConfigured {
                    Text("El acceso con Google necesita registrar la aplicación iOS en Firebase y añadir su configuración de Google. Mientras se completa, puedes usar una cuenta de correo y contraseña.")
                        .font(.footnote).foregroundStyle(BiblionTheme.secondary)
                    }
                }
            }.navigationTitle(register ? "Crear cuenta" : "Bienvenido a Biblion").navigationBarTitleDisplayMode(.inline)
            .toolbar { Button("Cerrar") { dismiss() } }
            .interactiveDismissDisabled(busy)
        }
    }
    private func authenticate() {
        busy = true; error = nil
        Task {
            defer { busy = false }
            do { try await cloud.authenticate(email: email.trimmingCharacters(in: .whitespaces), password: password, register: register); password = ""; dismiss() }
            catch { self.error = error.localizedDescription; password = "" }
        }
    }
}
struct EditProfileScreen: View {
    @EnvironmentObject private var cloud: CloudAccount
    @Environment(\.dismiss) private var dismiss
    @State private var names = ""
    @State private var surnames = ""
    @State private var alias = ""
    @State private var biography = ""
    @State private var avatar = "4278939295"
    @State private var showPhotoPicker = false
    @State private var photoData: Data?
    @State private var removePhoto = false
    @State private var busy = false
    @State private var error: String?
    var body: some View {
        NavigationStack {
            Form {
                Section("Identidad") {
                    TextField("Nombres", text: $names).textContentType(.givenName)
                    TextField("Apellidos", text: $surnames).textContentType(.familyName)
                    TextField("Alias", text: $alias).textInputAutocapitalization(.never)
                    TextField("Biografía", text: $biography, axis: .vertical).lineLimit(3...6)
                }
                Section("Avatar") {
                    Picker("Color", selection: $avatar) {
                        Text("Azul Biblion").tag("4278939295"); Text("Dorado").tag("4288766763"); Text("Azul profundo").tag("4279316819")
                    }
                    Button { showPhotoPicker = true } label: { Label(photoData == nil ? "Seleccionar foto" : "Foto seleccionada", systemImage: "photo") }
                    if cloud.profile["fotoPerfil"]?.isEmpty == false || photoData != nil {
                        Button(removePhoto ? "Se quitará al guardar" : "Quitar foto", role: .destructive) { removePhoto = true; photoData = nil }
                    }
                }
                if let error { Text(error).foregroundStyle(.red) }
                if busy { ProgressView("Guardando perfil…") }
            }.navigationTitle("Actualizar perfil").navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancelar") { dismiss() }.disabled(busy) }
                ToolbarItem(placement: .confirmationAction) { Button("Guardar") { save() }.disabled(busy || names.trimmingCharacters(in: .whitespaces).isEmpty || surnames.trimmingCharacters(in: .whitespaces).isEmpty || alias.trimmingCharacters(in: .whitespaces).isEmpty) }
            }
            .task { names = cloud.profile["nombres"] ?? ""; surnames = cloud.profile["apellidos"] ?? ""; alias = cloud.profile["alias"] ?? ""; biography = cloud.profile["biografia"] ?? ""; avatar = cloud.profile["avatarColor"] ?? avatar }
            .sheet(isPresented: $showPhotoPicker) { NativePhotoPicker { data in
                if let data, let image = UIImage(data: data), let jpg = image.jpegData(compressionQuality: 0.75), jpg.count < 5 * 1024 * 1024 { photoData = jpg; removePhoto = false }
                else if data != nil { error = "La foto debe tener menos de 5 MB." }
            } }
            .interactiveDismissDisabled(busy)
        }
    }
    private func save() {
        busy = true; error = nil
        Task {
            defer { busy = false }
            do {
                try await cloud.saveProfile(["nombres": names.trimmingCharacters(in: .whitespaces), "apellidos": surnames.trimmingCharacters(in: .whitespaces), "alias": String(alias.prefix(50)), "biografia": String(biography.prefix(1200))])
                try await cloud.saveAvatar(avatar)
                if removePhoto { try await cloud.clearPhoto() }
                if let photoData { try await cloud.uploadPhoto(photoData) }
                dismiss()
            } catch { self.error = error.localizedDescription }
        }
    }
}

struct ReadingGuideScreen: View {
    @EnvironmentObject private var app: AppModel
    @AppStorage("readingGuideCompleted") private var completed = false
    @AppStorage("readingGuideStep") private var step = 0
    @AppStorage("readingGuideBook") private var selectedBook = 0
    @AppStorage("readingGuideChapter") private var selectedChapter = 1
    @Environment(\.dismiss) private var dismiss
    @State private var testament: Testament = .old
    @State private var showVersions = false
    @State private var showBibi = false
    private let steps: [(String, String)] = [
        ("Bienvenido a Biblion", "Soy Bibi. Vamos a recorrer tu Biblia. Toca Comenzar para practicar con sus herramientas."),
        ("Elige un testamento", "La Biblia se organiza en Antiguo y Nuevo Testamento. Toca uno para explorar sus libros."),
        ("Elige un libro", "Los libros aparecen en orden bíblico. Toca el que quieras leer."),
        ("Elige un capítulo", "Toca un número para abrir el capítulo."),
        ("Tu espacio de lectura", "Este es el texto real de tu Biblia. Puedes desplazarte para leer. Cuando estés listo, continuamos."),
        ("Resalta un versículo", "Mantén pulsado un versículo y elige Resaltar o un color. Tu selección quedará guardada."),
        ("Explora las versiones", "Toca Elegir versión y selecciona una traducción. Biblion conserva el libro y el capítulo cuando están disponibles."),
        ("Conoce a Bibi", "Toca Abrir Bibi para conversar sobre el pasaje, un personaje o una palabra bíblica."),
        ("Pregunta y comprende", "Puedes escribir tu pregunta en Bibi. Al cerrar la conversación seguiremos el recorrido."),
        ("Más allá de la lectura", "En Explorar encontrarás el diccionario y los temas. El icono de modo estudio anuncia una función que llegará más adelante."),
        ("Tu Biblia te espera", "Puedes repetir este recorrido cuando quieras desde Perfil. ¡Disfruta la lectura!")]
    private var location: ReadingLocation { ReadingLocation(version: app.state.selectedVersion, book: selectedBook, chapter: selectedChapter, verse: 1) }
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                VStack(alignment: .leading, spacing: 10) {
                    Label("Bibi · \(min(step,10) + 1) de 11", systemImage: "bubble.left.and.bubble.right.fill").font(.caption.bold()).foregroundStyle(BiblionTheme.gold)
                    Text(steps[min(step,10)].0).font(.title2.bold()).accessibilityAddTraits(.isHeader)
                    Text(steps[min(step,10)].1).font(.body).fixedSize(horizontal: false, vertical: true)
                    guideAction
                }.padding(20).frame(maxWidth: .infinity, alignment: .leading).background(BiblionTheme.surface)
                Group {
                    switch step {
                    case 1:
                        List(Testament.allCases) { item in Button(item.title) { testament = item; step = 2 } }
                    case 2:
                        List(app.books().filter { testament.contains($0) }) { book in Button(book.name) { selectedBook = book.id; step = 3 } }
                    case 3:
                        ScrollView {
                            LazyVGrid(columns: [GridItem(.adaptive(minimum: 64))], spacing: 16) {
                                ForEach(app.books().first(where: { $0.id == selectedBook })?.chapters ?? [], id: \.self) { chapter in
                                    Button { selectedChapter = chapter; step = 4 } label: { Text("\(chapter)").frame(maxWidth: .infinity, minHeight: 56).background(BiblionTheme.surface, in: RoundedRectangle(cornerRadius: 12)) }
                                }
                            }.padding(20)
                        }
                    case 4...8: ReaderScreen(location: location).id(step)
                    default:
                        VStack(spacing: 22) {
                            Image(systemName: step == 9 ? "square.and.pencil" : "book.closed.fill").font(.system(size: 64)).foregroundStyle(BiblionTheme.gold)
                            Text(step == 9 ? "Modo estudio · Próximamente" : "La Palabra, cada día.").font(.title2).multilineTextAlignment(.center)
                        }.padding(24).frame(maxWidth: .infinity, maxHeight: .infinity)
                    }
                }
            }.navigationTitle("Recorrido de lectura").navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Más tarde") { dismiss() } } }
            .onChange(of: app.state.highlights) { _, values in
                if step == 5 && values.contains(where: { $0.location.book == selectedBook && $0.location.chapter == selectedChapter }) { step = 6 }
            }
            .sheet(isPresented: $showVersions) {
                VersionPicker(selected: app.state.selectedVersion, location: location) { version in app.selectVersion(version); step = 7 }
            }
            .sheet(isPresented: $showBibi, onDismiss: { if step == 8 { step = 9 } }) {
                NavigationStack { BibiScreen(location: location).toolbar { ToolbarItem(placement: .cancellationAction) { Button("Seguir recorrido") { showBibi = false } } } }
            }
            .biblionPage()
        }
    }
    @ViewBuilder private var guideAction: some View {
        switch step {
        case 0: Button("Comenzar recorrido") { step = 1 }.buttonStyle(.borderedProminent).foregroundStyle(.white)
        case 4: Button("Continuar") { step = 5 }.buttonStyle(.borderedProminent).foregroundStyle(.white)
        case 6: Button("Elegir versión") { showVersions = true }.buttonStyle(.borderedProminent).foregroundStyle(.white)
        case 7: Button("Abrir Bibi") { step = 8; showBibi = true }.buttonStyle(.borderedProminent).foregroundStyle(.white)
        case 9: Button("Continuar") { step = 10 }.buttonStyle(.borderedProminent).foregroundStyle(.white)
        case 10:
            Button("Comenzar a leer") { completed = true; step = 0; dismiss() }.buttonStyle(.borderedProminent).foregroundStyle(.white)
            Button("Ver recorrido nuevamente") { step = 0 }
        default: EmptyView()
        }
    }
}

struct NativePhotoPicker: UIViewControllerRepresentable {
    let onSelect: (Data?) -> Void
    func makeCoordinator() -> Coordinator { Coordinator(onSelect: onSelect) }
    func makeUIViewController(context: Context) -> PHPickerViewController {
        var config = PHPickerConfiguration(); config.filter = .images; config.selectionLimit = 1
        let picker = PHPickerViewController(configuration: config); picker.delegate = context.coordinator; return picker
    }
    func updateUIViewController(_ controller: PHPickerViewController, context: Context) {}
    @MainActor final class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let onSelect: (Data?) -> Void
        init(onSelect: @escaping (Data?) -> Void) { self.onSelect = onSelect }
        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            picker.dismiss(animated: true)
            guard let provider = results.first?.itemProvider else { return }
            provider.loadDataRepresentation(forTypeIdentifier: "public.image") { data, _ in
                Task { @MainActor in self.onSelect(data) }
            }
        }
    }
}
#endif
