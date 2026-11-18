#if canImport(SwiftUI)
import SwiftUI
import BiblionCore

@MainActor
final class AppModel: ObservableObject {
    @Published private(set) var state = ReadingState()
    @Published private(set) var versions: [BibleVersion] = []
    @Published private(set) var isReady = false
    @Published private(set) var startupError: String?
    @Published var saveError: String?
    private(set) var database: BibleDatabase?
    private(set) var library: KnowledgeLibrary?
    private var store: ReadingStateStore?
    private var catalogs: [String: [BibleBook]] = [:]
    private var saveTask: Task<Void, Never>?
    private var isStarting = false
    private var applyingRemote = false
    @Published private(set) var activeOwner: String?
    func switchAccount(_ uid: String?) async {
        guard isReady, uid != activeOwner else { return }
        saveTask?.cancel()
        do {
            try await store?.save(state)
            let base = try ReadingStateStore.applicationURL()
            let url = uid.map { base.deletingLastPathComponent().appendingPathComponent("reading-account-\($0).json") } ?? base
            let next = ReadingStateStore(url: url)
            let restored = try await next.load()
            store = next; state = restored; activeOwner = uid
        } catch { saveError = "No pudimos abrir los datos de esta cuenta. Conservamos tus archivos." }
    }

    var colorScheme: ColorScheme? {
        switch state.appearance { case .light: .light; case .dark: .dark; case .system: nil }
    }
    func version(_ key: String) -> BibleVersion? { versions.first { $0.id == key } }
    func books(version: String? = nil) -> [BibleBook] { catalogs[version ?? state.selectedVersion] ?? [] }
    func isAvailable(_ location: ReadingLocation, version: String) -> Bool {
        books(version: version).contains { $0.id == location.book && $0.chapters.contains(location.chapter) }
    }

    func start() async {
        guard !isReady, !isStarting else { return }
        isStarting = true
        startupError = nil
        defer { isStarting = false }
        do {
            let store = ReadingStateStore(url: try ReadingStateStore.applicationURL())
            var restored = try await store.load()
            let database = try BibleDatabase()
            let versions = try await database.versions()
            guard !versions.isEmpty else { throw BibleError.missingDatabase }
            var catalogs: [String: [BibleBook]] = [:]
            for version in versions { catalogs[version.id] = try await database.books(version: version.id) }
            if !versions.contains(where: { $0.id == restored.selectedVersion }) {
                restored.selectedVersion = versions[0].id
            }
            self.store = store
            self.database = database
            self.library = try KnowledgeLibrary()
            self.catalogs = catalogs
            self.versions = versions
            self.state = restored
            self.isReady = true
        } catch {
            startupError = "No pudimos preparar tu biblioteca. Tus datos guardados se conservarán. \(error.localizedDescription)"
        }
    }

    func update(_ change: (inout ReadingState) -> Void) {
        guard isReady else { return }
        let old = state
        change(&state)
        state.normalize()
        if !applyingRemote {
            let now = Date().timeIntervalSince1970 * 1000
            var times = state.syncTimes ?? [:]
            if old.appearance != state.appearance { times["darkModeUpdatedAt"] = now }
            if old.fontSize != state.fontSize { times["readerFontSizeUpdatedAt"] = now }
            if old.selectedVersion != state.selectedVersion { times["selectedBibleVersionUpdatedAt"] = now }
            if old.highlights != state.highlights {
                let changed = Set(old.highlights.filter { !state.highlights.contains($0) }.map(\.location) + state.highlights.filter { !old.highlights.contains($0) }.map(\.location))
                for location in changed { times["highlight:\(location.book):\(location.chapter)"] = now }
            }
            state.syncTimes = times
        }
        scheduleSave()
    }
    func applyRemote(_ change: (inout ReadingState) -> Void) {
        applyingRemote = true; update(change); applyingRemote = false
    }
    func selectVersion(_ key: String) { update { $0.selectedVersion = key } }
    func remember(_ location: ReadingLocation) { update { $0.lastLocation = location } }
    func highlighted(_ location: ReadingLocation) -> Bool {
        state.highlights.contains { $0.location == location }
    }
    func toggleHighlight(_ location: ReadingLocation, text: String) {
        update { $0.toggleHighlight(location: location, text: text) }
    }
    func highlight(_ location: ReadingLocation, text: String, color: UInt32) {
        update { state in
            state.highlights.removeAll { $0.location == location }
            state.highlights.insert(SavedVerse(location: location, text: text, color: color), at: 0)
        }
    }
    private func scheduleSave() {
        saveTask?.cancel()
        saveTask = Task {
            do { try await Task.sleep(for: .milliseconds(200)) } catch { return }
            await flush()
        }
    }
    func flush() async {
        guard let store, isReady else { return }
        let snapshot = state
        do { try await store.save(snapshot) }
        catch { saveError = "No pudimos guardar tu lectura en el dispositivo. Inténtalo de nuevo." }
    }
}

@MainActor
final class ReaderModel: ObservableObject {
    @Published var location: ReadingLocation
    @Published private(set) var chapter: BibleChapter?
    @Published private(set) var isLoading = true
    @Published private(set) var error: String?
    init(location: ReadingLocation) { self.location = location }

    func load(using app: AppModel) async {
        isLoading = true
        error = nil
        chapter = nil
        guard let database = app.database else {
            error = "La biblioteca todavía no está lista."
            isLoading = false
            return
        }
        let requested = location
        do {
            let loaded = try await database.chapter(at: requested)
            try Task.checkCancellation()
            guard requested == location else { return }
            chapter = loaded
            isLoading = false
            app.remember(requested)
        } catch is CancellationError {
            return
        } catch {
            guard requested == location else { return }
            self.error = error.localizedDescription
            isLoading = false
        }
    }
}
#endif
