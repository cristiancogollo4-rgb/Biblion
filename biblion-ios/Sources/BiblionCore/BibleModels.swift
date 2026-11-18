import Foundation

public struct BibleVersion: Identifiable, Hashable, Sendable {
    public let id: String
    public let bookCount: Int
    public var abbreviation: String { id == "rv1960" ? "RVR1960" : id.uppercased() }
    public var name: String {
        switch id {
        case "rv1960": "Reina-Valera 1960"
        case "nvi": "Nueva Versión Internacional"
        case "dhh": "Dios Habla Hoy"
        case "ntv": "Nueva Traducción Viviente"
        case "tla": "Traducción en Lenguaje Actual"
        case "pdt": "Palabra de Dios para Todos"
        default: id.uppercased()
        }
    }
}

public enum Testament: String, CaseIterable, Identifiable, Sendable {
    case old, new
    public var id: String { rawValue }
    public var title: String { self == .old ? "Antiguo Testamento" : "Nuevo Testamento" }
    public var shortTitle: String { self == .old ? "Antiguo" : "Nuevo" }
    public func contains(_ book: BibleBook) -> Bool { self == .old ? book.id < 39 : book.id >= 39 }
}

public struct BibleBook: Identifiable, Hashable, Sendable {
    public let id: Int
    public let name: String
    public let chapters: [Int]
    public var testament: Testament { id < 39 ? .old : .new }
    public static let canonicalNames = [
        "Génesis", "Éxodo", "Levítico", "Números", "Deuteronomio", "Josué", "Jueces", "Rut",
        "1 Samuel", "2 Samuel", "1 Reyes", "2 Reyes", "1 Crónicas", "2 Crónicas", "Esdras",
        "Nehemías", "Ester", "Job", "Salmos", "Proverbios", "Eclesiastés", "Cantares", "Isaías",
        "Jeremías", "Lamentaciones", "Ezequiel", "Daniel", "Oseas", "Joel", "Amós", "Abdías",
        "Jonás", "Miqueas", "Nahúm", "Habacuc", "Sofonías", "Hageo", "Zacarías", "Malaquías",
        "Mateo", "Marcos", "Lucas", "Juan", "Hechos", "Romanos", "1 Corintios", "2 Corintios",
        "Gálatas", "Efesios", "Filipenses", "Colosenses", "1 Tesalonicenses", "2 Tesalonicenses",
        "1 Timoteo", "2 Timoteo", "Tito", "Filemón", "Hebreos", "Santiago", "1 Pedro",
        "2 Pedro", "1 Juan", "2 Juan", "3 Juan", "Judas", "Apocalipsis"
    ]
    public static func displayName(for index: Int) -> String {
        canonicalNames.indices.contains(index) ? canonicalNames[index] : "Libro \(index + 1)"
    }
}

public struct ReadingLocation: Codable, Hashable, Sendable {
    public var version: String
    public var book: Int
    public var chapter: Int
    public var verse: Int?
    public init(version: String = "rv1960", book: Int = 0, chapter: Int = 1, verse: Int? = nil) {
        self.version = version; self.book = book; self.chapter = chapter; self.verse = verse
    }
    public var reference: String {
        "\(BibleBook.displayName(for: book)) \(chapter)" + (verse.map { ":\($0)" } ?? "")
    }
}

public struct BibleVerse: Identifiable, Hashable, Sendable {
    public let number: Int
    public let text: String
    public let title: String?
    public var id: Int { number }
}

public struct BibleChapter: Sendable {
    public let location: ReadingLocation
    public let verses: [BibleVerse]
}

public enum ChapterNavigation {
    // Uses the actual catalog: some bundled translations omit entire books or chapters.
    public static func adjacent(to location: ReadingLocation, in books: [BibleBook], forward: Bool) -> ReadingLocation? {
        let ordered = books.sorted { $0.id < $1.id }
        guard let bookOffset = ordered.firstIndex(where: { $0.id == location.book }),
              let chapterOffset = ordered[bookOffset].chapters.firstIndex(of: location.chapter) else { return nil }
        let nextChapter = chapterOffset + (forward ? 1 : -1)
        if ordered[bookOffset].chapters.indices.contains(nextChapter) {
            return ReadingLocation(version: location.version, book: location.book, chapter: ordered[bookOffset].chapters[nextChapter])
        }
        let nextBook = bookOffset + (forward ? 1 : -1)
        guard ordered.indices.contains(nextBook),
              let chapter = forward ? ordered[nextBook].chapters.first : ordered[nextBook].chapters.last else { return nil }
        return ReadingLocation(version: location.version, book: ordered[nextBook].id, chapter: chapter)
    }
}
