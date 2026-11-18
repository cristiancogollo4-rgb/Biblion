#if canImport(SwiftUI)
import SwiftUI
import BiblionCore

struct HomeScreen: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.colorScheme) private var colorScheme
    @State private var meditation: BibleVerse?
    @State private var showVersions = false
    @AppStorage("readingGuideCompleted") private var guideCompleted = false
    @State private var showGuide = false
    private var meditationLocation: ReadingLocation {
        ReadingLocation(version: app.state.selectedVersion, book: 42, chapter: 3, verse: 16)
    }
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                HStack(spacing: 4) {
                    Image(colorScheme == .dark ? "logo_biblion_dark" : "logo_biblion_light", bundle: .module)
                        .resizable().scaledToFit().frame(width: 68, height: 68).accessibilityHidden(true)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Biblion").font(.title2.weight(.semibold))
                        Text("Tu espacio para la Palabra").font(.subheadline).foregroundStyle(BiblionTheme.secondary)
                    }
                }
                VStack(alignment: .leading, spacing: 20) {
                    Text("La Palabra,\ncada día.")
                        .font(.system(.largeTitle, design: .serif, weight: .semibold)).fixedSize(horizontal: false, vertical: true)
                        .accessibilityAddTraits(.isHeader)
                    Text("Abre tu Biblia. Encuentra un momento para leer, comprender y reflexionar.")
                        .font(.body).foregroundStyle(.white.opacity(0.9))
                    NavigationLink(value: AppRoute.reader(resumeLocation)) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(app.state.lastLocation == nil ? "Comenzar a leer" : "Continuar leyendo")
                                    .font(.headline)
                                Text("\(resumeLocation.reference) · \(app.version(resumeLocation.version)?.abbreviation ?? "")").font(.subheadline)
                            }
                            Spacer(minLength: 8)
                            Image(systemName: "arrow.right").accessibilityHidden(true)
                        }.padding(16).foregroundStyle(BiblionTheme.hero)
                            .background(Color(red: 0.95, green: 0.82, blue: 0.48), in: RoundedRectangle(cornerRadius: 14))
                    }.buttonStyle(.plain)
                }
                .padding(24).foregroundStyle(.white)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(BiblionTheme.hero, in: RoundedRectangle(cornerRadius: 24))

                VStack(alignment: .leading, spacing: 12) {
                    Text("Explorar la Biblia").font(.title2.weight(.semibold)).accessibilityAddTraits(.isHeader)
                    ForEach(Testament.allCases) { testament in
                        NavigationLink(value: AppRoute.books(testament)) {
                            HStack(spacing: 16) {
                                Image(systemName: testament == .old ? "book.closed" : "book")
                                    .font(.title2).foregroundStyle(BiblionTheme.gold).accessibilityHidden(true)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text(testament.title).font(.headline)
                                    Text("\(app.books().filter { testament.contains($0) }.count) libros disponibles")
                                        .font(.subheadline).foregroundStyle(BiblionTheme.secondary)
                                }
                                Spacer(minLength: 8)
                                Image(systemName: "chevron.right").foregroundStyle(BiblionTheme.secondary).accessibilityHidden(true)
                            }.padding(20).background(BiblionTheme.surface, in: RoundedRectangle(cornerRadius: 18))
                        }.buttonStyle(.plain)
                    }
                }
                if let meditation {
                    VStack(alignment: .leading, spacing: 14) {
                        Label("Para meditar", systemImage: "sun.max").font(.subheadline.weight(.semibold))
                            .foregroundStyle(BiblionTheme.gold)
                        Text(meditation.text).font(.system(.title3, design: .serif)).lineSpacing(6)
                        NavigationLink(value: AppRoute.reader(meditationLocation)) {
                            HStack {
                                Text("Juan 3:16 · \(app.version(app.state.selectedVersion)?.abbreviation ?? "")")
                                Image(systemName: "arrow.up.right").accessibilityHidden(true)
                            }.font(.subheadline.weight(.semibold)).frame(minHeight: 44)
                        }
                    }.padding(24).frame(maxWidth: .infinity, alignment: .leading)
                        .background(BiblionTheme.surface, in: RoundedRectangle(cornerRadius: 18))
                }
                Label("Tu Biblia disponible sin conexión", systemImage: "checkmark.circle")
                    .font(.footnote).foregroundStyle(BiblionTheme.secondary)
                NavigationLink { StudyComingSoonScreen() } label: {
                    Label("Modo estudio · Próximamente", systemImage: "square.and.pencil").frame(minHeight: 44)
                }
            }.padding(20).frame(maxWidth: 720).frame(maxWidth: .infinity)
        }
        .navigationTitle("Inicio").navigationBarTitleDisplayMode(.inline)
        .toolbar { ToolbarItem(placement: .topBarTrailing) {
            VersionButton(title: app.version(app.state.selectedVersion)?.abbreviation ?? "Versión") { showVersions = true }
        } }
        .sheet(isPresented: $showVersions) { VersionPicker { app.selectVersion($0) } }
        .sheet(isPresented: $showGuide) { ReadingGuideScreen() }
        .task { if !guideCompleted { showGuide = true } }
        .task(id: app.state.selectedVersion) {
            meditation = nil
            guard let database = app.database else { return }
            let location = meditationLocation
            if let chapter = try? await database.chapter(at: location), !Task.isCancelled {
                meditation = chapter.verses.first { $0.number == 16 }
            }
        }
        .biblionPage()
    }
    private var resumeLocation: ReadingLocation {
        app.state.lastLocation ?? ReadingLocation(version: app.state.selectedVersion)
    }
}

#endif
