#if canImport(SwiftUI)
import SwiftUI
import BiblionCore

struct ExploreScreen: View {
    var body: some View {
        List {
            Section("Comprender la Palabra") {
                NavigationLink { LibraryScreen(topics: false) } label: { Label("Diccionario bíblico", systemImage: "text.book.closed") }
                NavigationLink { LibraryScreen(topics: true) } label: { Label("Explorar temas", systemImage: "square.grid.2x2") }
                NavigationLink { BibiScreen(location: nil) } label: { Label("Conversar con Bibi", systemImage: "bubble.left.and.bubble.right") }
            }
            Section("Tu biblioteca") {
                NavigationLink { HighlightsScreen() } label: { Label("Mis resaltados", systemImage: "bookmark") }
                NavigationLink { StudyComingSoonScreen() } label: {
                    HStack { Label("Modo estudio", systemImage: "square.and.pencil"); Spacer(); Text("Próximamente").font(.caption).foregroundStyle(BiblionTheme.secondary) }
                }
            }
        }.navigationTitle("Explorar").biblionPage()
    }
}

struct StudyComingSoonScreen: View {
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "square.and.pencil").font(.system(size: 56)).foregroundStyle(BiblionTheme.gold)
            Text("Modo estudio").font(.largeTitle.bold())
            Text("Próximamente").font(.headline).foregroundStyle(BiblionTheme.blue)
            Text("Un espacio para preparar tus enseñanzas y organizar tus reflexiones con la Biblia. Estamos preparando esta función para iOS.")
                .multilineTextAlignment(.center).foregroundStyle(BiblionTheme.secondary)
        }.padding(32).frame(maxWidth: 600).frame(maxWidth: .infinity, maxHeight: .infinity).biblionPage()
    }
}

struct SearchScreen: View {
    @EnvironmentObject private var app: AppModel
    @State private var query = ""
    @State private var testament = -1
    @State private var book = -1
    @State private var results: [VerseResult] = []
    @State private var loading = false
    @State private var error: String?
    @State private var more = false
    @State private var page = 0
    @AppStorage("recentVerseQueries") private var recentJSON = "[]"
    private var recent: [String] { (try? JSONDecoder().decode([String].self, from: Data(recentJSON.utf8))) ?? [] }
    private var key: String { "\(query)|\(testament)|\(book)|\(app.state.selectedVersion)|\(page)" }
    var body: some View {
        List {
            Section {
                Picker("Testamento", selection: $testament) {
                    Text("Toda la Biblia").tag(-1); Text("Antiguo").tag(0); Text("Nuevo").tag(1)
                }
                Picker("Libro", selection: $book) {
                    Text("Todos los libros").tag(-1)
                    ForEach(app.books().filter { testament == -1 || (testament == 0 ? $0.id < 39 : $0.id >= 39) }) { Text($0.name).tag($0.id) }
                }
            }
            if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                if !recent.isEmpty {
                    Section("Búsquedas recientes") {
                        ForEach(recent, id: \.self) { item in Button(item) { query = item } }
                        Button("Borrar historial", role: .destructive) { recentJSON = "[]" }
                    }
                }
                Section("Versículos para comenzar") {
                    ForEach(popular, id: \.0) { item in
                        NavigationLink(value: AppRoute.reader(ReadingLocation(version: app.state.selectedVersion, book: item.1, chapter: item.2, verse: item.3))) {
                            VStack(alignment: .leading, spacing: 5) { Text(item.0).font(.headline); Text("\(BibleBook.displayName(for: item.1)) \(item.2):\(item.3)").foregroundStyle(BiblionTheme.secondary) }
                        }
                    }
                }
            } else {
                if loading { ProgressView("Buscando en tu Biblia…") }
                if let error { Text(error).foregroundStyle(.red) }
                if !loading && results.isEmpty && error == nil { Text("No encontramos versículos. Prueba otras palabras o cambia los filtros.").foregroundStyle(BiblionTheme.secondary) }
                Section("Resultados · \(app.version(app.state.selectedVersion)?.abbreviation ?? "")") {
                    ForEach(results) { verse in VerseResultRow(verse: verse) }
                    if more { Button("Mostrar más") { page += 1 }.disabled(loading) }
                }
            }
        }
        .navigationTitle("Buscar").searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Buscar palabras en la Biblia")
        .onSubmit(of: .search) { remember() }
        .onDisappear { remember() }
        .onChange(of: query) { _, _ in page = 0 }
        .onChange(of: testament) { _, _ in book = -1; page = 0 }
        .onChange(of: book) { _, _ in page = 0 }
        .task(id: key) { await search() }
        .biblionPage()
    }
    private let popular: [(String, Int, Int, Int)] = [("Amor",42,3,16),("Fe",57,11,1),("Gracia",48,2,8),("Salvación",44,10,9),("Esperanza",23,29,11),("Paz",49,4,7)]
    private func remember() {
        let value = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else { return }
        let list = [value] + recent.filter { $0 != value }
        if let data = try? JSONEncoder().encode(Array(list.prefix(20))) { recentJSON = String(decoding: data, as: UTF8.self) }
    }
    private func search() async {
        loading = true; error = nil
        if page == 0 { results = [] }
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { loading = false; more = false; return }
        do {
            try await Task.sleep(for: .milliseconds(300))
            guard let db = app.database else { return }
            let hits = try await db.search(query, version: app.state.selectedVersion, testament: testament < 0 ? nil : testament, book: book < 0 ? nil : book, offset: page * 60)
            try Task.checkCancellation()
            if page == 0 { results = hits } else { results += hits }
            more = hits.count == 60; loading = false
        } catch is CancellationError { }
        catch { self.error = error.localizedDescription; loading = false }
    }
}

struct VerseResultRow: View {
    let verse: VerseResult
    var body: some View {
        NavigationLink(value: AppRoute.reader(verse.location)) {
            VStack(alignment: .leading, spacing: 8) {
                Text(verse.location.reference).font(.headline).foregroundStyle(BiblionTheme.blue)
                Text(verse.text).font(.body).foregroundStyle(BiblionTheme.ink).lineLimit(5)
            }.padding(.vertical, 6)
        }
    }
}

struct LibraryScreen: View {
    @EnvironmentObject private var app: AppModel
    let topics: Bool
    @State private var query = ""
    @State private var category = ""
    @State private var entries: [KnowledgeEntry] = []
    @State private var error: String?
    @State private var loading = true
    private var categories: [(String, String)] {
        topics ? [("BOOK","Libros"),("PERSON","Personas"),("PLACE","Lugares"),("EVENT","Eventos"),("ATTRIBUTE_OF_GOD","Atributos de Dios"),("PROPHECY","Profecías"),("COMPARATIVE_RELIGION","Religiones"),("SIN","Pecado"),("DOCTRINE","Doctrina"),("CHURCH","Iglesia"),("CHRISTIAN_LIFE","Vida cristiana")] : [("person","Personas"),("place","Lugares"),("concept","Conceptos"),("object","Objetos"),("practice","Prácticas"),("event","Eventos"),("book","Libros"),("other","General")]
    }
    var body: some View {
        List {
            Picker("Categoría", selection: $category) {
                Text("Todas las categorías").tag("")
                ForEach(categories, id: \.0) { Text($0.1).tag($0.0) }
            }
            if loading { ProgressView("Abriendo biblioteca…") }
            if let error { Text(error).foregroundStyle(.red) }
            Section("\(entries.count) entradas") {
                ForEach(entries) { entry in
                    NavigationLink { EntryDetailScreen(entry: entry, topics: topics) } label: {
                        VStack(alignment: .leading, spacing: 5) {
                            Text(entry.title).font(.headline)
                            Text(entry.definition).font(.subheadline).foregroundStyle(BiblionTheme.secondary).lineLimit(2)
                        }.padding(.vertical, 5)
                    }
                }
                if !loading && entries.isEmpty { Text("No encontramos entradas. Prueba otro término.") }
            }
        }.navigationTitle(topics ? "Explorar temas" : "Diccionario")
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: topics ? "Buscar un tema" : "Buscar una palabra o personaje")
        .task(id: "\(query)|\(category)") {
            loading = true; error = nil
            do {
                try await Task.sleep(for: .milliseconds(200))
                guard let library = app.library else { throw BibleError.missingDatabase }
                let loaded = try await library.entries(query: query, category: category.isEmpty ? nil : category, topics: topics)
                try Task.checkCancellation(); entries = loaded; loading = false
            } catch is CancellationError { } catch { self.error = error.localizedDescription; loading = false }
        }.biblionPage()
    }
}

struct EntryDetailScreen: View {
    @EnvironmentObject private var app: AppModel
    let entry: KnowledgeEntry
    var topics = false
    @State private var references: [TopicReference] = []
    @State private var children: [KnowledgeEntry] = []
    @State private var error: String?
    var body: some View {
        List {
            Section {
                Text(entry.title).font(.system(.largeTitle, design: .serif)).accessibilityAddTraits(.isHeader)
                Text(entry.definition.isEmpty ? "Esta entrada dispone de los siguientes datos de referencia." : entry.definition).textSelection(.enabled)
                ForEach(entry.metadata, id: \.self) { Text($0).foregroundStyle(BiblionTheme.secondary) }
            }
            if let error { Text(error).foregroundStyle(.red) }
            if !references.isEmpty {
                Section("Pasajes relacionados") {
                    ForEach(references) { ref in
                        if let location = ref.location(version: app.state.selectedVersion) { NavigationLink(ref.label, value: AppRoute.reader(location)) }
                        else { Text(ref.label) }
                    }
                }
            }
            if !children.isEmpty {
                Section("Temas relacionados") {
                    ForEach(children) { child in NavigationLink(child.title) { EntryDetailScreen(entry: child, topics: true) } }
                }
            }
            NavigationLink { BibiScreen(location: nil, initialQuestion: "Profundiza sobre \(entry.title)") } label: { Label("Profundizar con Bibi", systemImage: "bubble.left.and.bubble.right") }
        }.navigationTitle(entry.title).navigationBarTitleDisplayMode(.inline)
        .task {
            guard topics, let library = app.library else { return }
            do { references = try await library.topicReferences(entry.id); children = try await library.children(entry.id) }
            catch { self.error = error.localizedDescription }
        }.biblionPage()
    }
}

struct CrossReferencesScreen: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.dismiss) private var dismiss
    let location: ReadingLocation
    @State private var references: [String] = []
    @State private var loading = true
    @State private var error: String?
    var body: some View {
        List {
            Section(location.reference) {
                if loading { ProgressView("Consultando referencias…") }
                if let error { Text(error).foregroundStyle(.red) }
                if !loading && references.isEmpty && error == nil { Text("No hay referencias relacionadas disponibles para este versículo.") }
                ForEach(references, id: \.self) { reference in
                    if let target = parse(reference) { NavigationLink(reference, value: AppRoute.reader(target)) }
                    else { Text(reference).textSelection(.enabled) }
                }
            }
        }.navigationTitle("Referencias relacionadas").navigationBarTitleDisplayMode(.inline)
        .toolbar { Button("Cerrar") { dismiss() } }
        .task {
            do { references = try await app.library?.crossReferences(location) ?? []; loading = false }
            catch { self.error = error.localizedDescription; loading = false }
        }.biblionPage()
    }
    private func parse(_ value: String) -> ReadingLocation? {
        guard let match = value.range(of: #"\s+\d+:\d+"#, options: .regularExpression) else { return nil }
        let name = String(value[..<match.lowerBound]).bookKey
        let numbers = value[match].trimmingCharacters(in: .whitespaces).split(separator: ":")
        guard let book = BibleBook.canonicalNames.firstIndex(where: { $0.bookKey == name }), numbers.count == 2, let chapter = Int(numbers[0]), let verse = Int(numbers[1]) else { return nil }
        return ReadingLocation(version: location.version, book: book, chapter: chapter, verse: verse)
    }
}

struct CompareVersionsScreen: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.dismiss) private var dismiss
    let location: ReadingLocation
    @State private var texts: [String: String] = [:]
    @State private var loading = true
    var body: some View {
        List {
            if loading { ProgressView("Consultando versiones…") }
            ForEach(app.versions) { version in
                Section(version.name) {
                    Text(texts[version.id] ?? "Este versículo no está disponible en esta versión.").textSelection(.enabled)
                }
            }
        }.navigationTitle(location.reference).navigationBarTitleDisplayMode(.inline)
        .toolbar { Button("Cerrar") { dismiss() } }
        .task {
            guard let db = app.database else { loading = false; return }
            for version in app.versions {
                var target = location; target.version = version.id
                if let chapter = try? await db.chapter(at: target), let verse = chapter.verses.first(where: { $0.number == location.verse }) { texts[version.id] = verse.text }
            }
            loading = false
        }.biblionPage()
    }
}
#endif
