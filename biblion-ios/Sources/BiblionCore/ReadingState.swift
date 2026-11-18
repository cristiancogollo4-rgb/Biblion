import Foundation

public enum AppAppearance: String, CaseIterable, Codable, Sendable {
    case light, dark, system
    public var title: String {
        switch self { case .light: "Claro"; case .dark: "Oscuro"; case .system: "Del dispositivo" }
    }
}

public struct SavedVerse: Codable, Identifiable, Hashable, Sendable {
    public let location: ReadingLocation
    public let text: String
    public let savedAt: Date
    public var color: UInt32?
    public var id: String { "\(location.version):\(location.book):\(location.chapter):\(location.verse ?? 0)" }
    public init(location: ReadingLocation, text: String, savedAt: Date = Date(), color: UInt32? = nil) {
        self.location = location; self.text = text; self.savedAt = savedAt; self.color = color
    }
}

public struct ReadingState: Codable, Equatable, Sendable {
    public var selectedVersion: String = "rv1960"
    public var appearance: AppAppearance = .light
    public var fontSize: Double = 20
    public var lastLocation: ReadingLocation?
    public var highlights: [SavedVerse] = []
    public var syncTimes: [String: Double]?
    public init() {}
    public mutating func normalize() {
        fontSize = fontSize.isFinite ? min(34, max(16, fontSize)) : 20
        if selectedVersion.isEmpty { selectedVersion = "rv1960" }
        var seen = Set<String>()
        highlights = highlights.filter { $0.location.verse != nil && seen.insert($0.id).inserted }
    }
    public mutating func toggleHighlight(location: ReadingLocation, text: String) {
        guard location.verse != nil else { return }
        let item = SavedVerse(location: location, text: text)
        if let index = highlights.firstIndex(where: { $0.id == item.id }) { highlights.remove(at: index) }
        else { highlights.insert(item, at: 0) }
    }
}

public actor ReadingStateStore {
    public let url: URL
    public init(url: URL) { self.url = url }
    public static func applicationURL() throws -> URL {
        try FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask,
                                   appropriateFor: nil, create: true)
            .appendingPathComponent("Biblion", isDirectory: true).appendingPathComponent("reading-v1.json")
    }
    public func load() throws -> ReadingState {
        guard FileManager.default.fileExists(atPath: url.path) else { return ReadingState() }
        var state = try JSONDecoder().decode(ReadingState.self, from: Data(contentsOf: url))
        state.normalize()
        return state
    }
    public func save(_ state: ReadingState) throws {
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        var validated = state
        validated.normalize()
        try JSONEncoder().encode(validated).write(to: url, options: .atomic)
    }
}
