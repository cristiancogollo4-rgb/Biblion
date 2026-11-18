#if canImport(SwiftUI)
import SwiftUI
import BiblionCore

@main
struct BiblionApp: App {
    @StateObject private var model = AppModel()
    @StateObject private var cloud = CloudAccount()
    @StateObject private var chats = ChatStore()
    @State private var syncTask: Task<Void, Never>?
    @Environment(\.scenePhase) private var scenePhase
    var body: some Scene {
        WindowGroup {
            Group {
                if model.isReady { BiblionRootView() }
                else if let error = model.startupError {
                    StatusView(title: "No pudimos abrir Biblion", message: error, symbol: "book.closed",
                               actionTitle: "Reintentar") { Task { await model.start() } }
                } else {
                    VStack(spacing: 24) {
                        Image(systemName: "book.closed.fill").font(.largeTitle).foregroundStyle(BiblionTheme.blue)
                        Text("Biblion").font(.largeTitle.weight(.semibold))
                        ProgressView("Preparando tu Biblia…")
                    }.frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .environmentObject(model)
            .environmentObject(cloud)
            .environmentObject(chats)
            .preferredColorScheme(model.colorScheme)
            .tint(BiblionTheme.blue)
            .background(BiblionTheme.paper.ignoresSafeArea())
            .task { await model.start() }
            .task(id: "\(model.isReady):\(cloud.session?.uid ?? "guest")") {
                if model.isReady { await model.switchAccount(cloud.session?.uid) }
            }
            .task(id: model.activeOwner) {
                guard model.activeOwner != nil else { return }
                while !Task.isCancelled {
                    do { try await cloud.synchronize(model) }
                    catch is CancellationError { return }
                    catch { cloud.message = error.localizedDescription }
                    do { try await Task.sleep(for: .seconds(30)) } catch { return }
                }
            }
            .onChange(of: model.state.syncTimes) { _, _ in
                syncTask?.cancel()
                guard cloud.session != nil else { return }
                syncTask = Task {
                    do {
                        try await Task.sleep(for: .milliseconds(800))
                        try await cloud.synchronize(model)
                    } catch is CancellationError { } catch { cloud.message = error.localizedDescription }
                }
            }
            .onChange(of: scenePhase) { _, phase in
                if phase != .active { Task { await model.flush() } }
            }
            .alert("No se guardaron los cambios", isPresented: Binding(
                get: { model.saveError != nil }, set: { if !$0 { model.saveError = nil } }
            )) {
                Button("Reintentar") { Task { await model.flush() } }
                Button("Cerrar", role: .cancel) {}
            } message: { Text(model.saveError ?? "") }
        }
    }
}

enum AppRoute: Hashable {
    case books(Testament)
    case chapters(Int)
    case reader(ReadingLocation)
}

struct BiblionRootView: View {
    var body: some View {
        TabView {
            NavigationStack { HomeScreen().biblionDestinations() }
                .tabItem { Label("Inicio", systemImage: "house") }
            NavigationStack { BooksScreen().biblionDestinations() }
                .tabItem { Label("Biblia", systemImage: "book") }
            NavigationStack { SearchScreen().biblionDestinations() }
                .tabItem { Label("Buscar", systemImage: "magnifyingglass") }
            NavigationStack { ExploreScreen().biblionDestinations() }
                .tabItem { Label("Explorar", systemImage: "square.grid.2x2") }
            NavigationStack { ProfileScreen().biblionDestinations() }
                .tabItem { Label("Perfil", systemImage: "person.crop.circle") }
        }
    }
}

extension View {
    func biblionDestinations() -> some View {
        navigationDestination(for: AppRoute.self) { route in
            switch route {
            case .books(let testament): BooksScreen(initialTestament: testament)
            case .chapters(let book): ChaptersScreen(bookIndex: book)
            case .reader(let location): ReaderScreen(location: location)
            }
        }
    }
}
#endif
