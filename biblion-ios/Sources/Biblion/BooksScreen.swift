#if canImport(SwiftUI)
import SwiftUI
import BiblionCore

struct BooksScreen: View {
    @EnvironmentObject private var app: AppModel
    @State private var testament: Testament
    @State private var query = ""
    @State private var showVersions = false
    @State private var description: KnowledgeEntry?
    init(initialTestament: Testament = .old) { _testament = State(initialValue: initialTestament) }
    private var filteredBooks: [BibleBook] {
        app.books().filter { testament.contains($0) && (query.isEmpty || $0.name.localizedStandardContains(query)) }
    }
    var body: some View {
        VStack(spacing: 0) {
            Picker("Testamento", selection: $testament) {
                ForEach(Testament.allCases) { Text($0.shortTitle).tag($0) }
            }.pickerStyle(.segmented).padding(.horizontal, 20).padding(.vertical, 12)
            .simultaneousGesture(DragGesture(minimumDistance: 60).onEnded { value in if abs(value.translation.width) > abs(value.translation.height) * 2 { testament = value.translation.width < 0 ? .new : .old } })
            if filteredBooks.isEmpty {
                StatusView(title: "No encontramos ese libro", message: "Prueba otro nombre o cambia de testamento.", symbol: "magnifyingglass")
            } else {
                List {
                    Section {
                        ForEach(filteredBooks) { book in
                            NavigationLink(value: AppRoute.chapters(book.id)) {
                                HStack(spacing: 16) {
                                    Text(String(format: "%02d", book.id + 1))
                                        .font(.subheadline.monospacedDigit()).foregroundStyle(BiblionTheme.gold)
                                        .frame(minWidth: 28).accessibilityHidden(true)
                                    VStack(alignment: .leading, spacing: 5) {
                                        Text(book.name).font(.headline)
                                        Text(book.chapters.count == 1 ? "1 capítulo" : "\(book.chapters.count) capítulos")
                                            .font(.subheadline).foregroundStyle(BiblionTheme.secondary)
                                    }
                                }.padding(.vertical, 8)
                            }.contextMenu {
                                Button("Descripción del libro", systemImage: "info.circle") {
                                    Task { description = try? await app.library?.bookDescription(book.name) }
                                }
                            }.listRowBackground(BiblionTheme.surface)
                        }
                    } header: { Text(testament.title) }
                }.listStyle(.insetGrouped).scrollContentBackground(.hidden)
            }
        }
        .navigationTitle("Biblia")
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: "Buscar un libro")
        .toolbar { ToolbarItem(placement: .topBarTrailing) {
            VersionButton(title: app.version(app.state.selectedVersion)?.abbreviation ?? "Versión") { showVersions = true }
        } }
        .sheet(isPresented: $showVersions) { VersionPicker { app.selectVersion($0) } }
        .sheet(item: $description) { entry in NavigationStack { EntryDetailScreen(entry: entry, topics: true).biblionDestinations() }.presentationDragIndicator(.visible) }
        .biblionPage()
    }
}

struct ChaptersScreen: View {
    @EnvironmentObject private var app: AppModel
    let bookIndex: Int
    @ScaledMetric(relativeTo: .body) private var cellWidth: CGFloat = 64
    @State private var showVersions = false
    private var book: BibleBook? { app.books().first { $0.id == bookIndex } }
    var body: some View {
        ScrollView {
            if let book {
                VStack(alignment: .leading, spacing: 24) {
                    VStack(alignment: .leading, spacing: 8) {
                        Text(book.testament.title).font(.subheadline).foregroundStyle(BiblionTheme.gold)
                        Text(book.name).font(.system(.largeTitle, design: .serif, weight: .semibold)).accessibilityAddTraits(.isHeader)
                        Text("Elige un capítulo para comenzar.").font(.body).foregroundStyle(BiblionTheme.secondary)
                    }
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: cellWidth), spacing: 12)], spacing: 12) {
                        ForEach(book.chapters, id: \.self) { number in
                            NavigationLink(value: AppRoute.reader(ReadingLocation(version: app.state.selectedVersion, book: book.id, chapter: number))) {
                                Text("\(number)").font(.title3.weight(.medium).monospacedDigit())
                                    .frame(maxWidth: .infinity, minHeight: cellWidth)
                                    .background(BiblionTheme.surface, in: RoundedRectangle(cornerRadius: 14))
                                    .overlay(RoundedRectangle(cornerRadius: 14).strokeBorder(BiblionTheme.divider, lineWidth: 1))
                            }.buttonStyle(.plain).accessibilityLabel("\(book.name), capítulo \(number)")
                        }
                    }
                }.padding(24).frame(maxWidth: 800).frame(maxWidth: .infinity, alignment: .center)
            } else {
                StatusView(title: "Libro no disponible", message: "Selecciona otra versión para leer este libro.", symbol: "book.closed")
            }
        }
        .navigationTitle(BibleBook.displayName(for: bookIndex)).navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement: .topBarTrailing) {
            VersionButton(title: app.version(app.state.selectedVersion)?.abbreviation ?? "Versión") { showVersions = true }
        } }
        .sheet(isPresented: $showVersions) { VersionPicker { app.selectVersion($0) } }
        .biblionPage()
    }
}
#endif
