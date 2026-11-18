import Foundation
import CSQLite

public struct VerseResult: Identifiable, Hashable, Sendable {
    public let location: ReadingLocation
    public let text: String
    public var id: String { "\(location.version):\(location.book):\(location.chapter):\(location.verse ?? 0)" }
}
public struct KnowledgeEntry: Identifiable, Hashable, Sendable {
    public let id: String
    public let title: String
    public let category: String
    public let definition: String
    public let metadata: [String]
}
public struct TopicReference: Identifiable, Hashable, Sendable {
    public let book: String
    public let chapter: Int
    public let start: Int
    public let end: Int
    public var id: String { "\(book) \(chapter):\(start)-\(end)" }
    public var label: String { start == end ? "\(book) \(chapter):\(start)" : id }
    public func location(version: String) -> ReadingLocation? {
        guard let book = BibleBook.canonicalNames.firstIndex(where: { $0.bookKey == self.book.bookKey }) else { return nil }
        return ReadingLocation(version: version, book: book, chapter: chapter, verse: start)
    }
}
public extension String {
    var bookKey: String {
        let value = searchKey.filter { $0.isLetter || $0.isNumber }
        return value == "cantardeloscantares" ? "cantares" : value
    }
    var searchKey: String { folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "es")).lowercased().trimmingCharacters(in: .whitespacesAndNewlines) }
}

public actor KnowledgeLibrary {
    private let dictionary: SQLiteConnection
    private let topicsDB: SQLiteConnection
    private let strongs: SQLiteConnection
    private let referencesDB: SQLiteConnection
    private var dictionaryEntries: [KnowledgeEntry] = []
    private var topicEntries: [KnowledgeEntry] = []
    public init() throws {
        func open(_ name: String) throws -> SQLiteConnection {
            guard let url = Bundle.module.url(forResource: name, withExtension: "db") else { throw BibleError.missingDatabase }
            return try SQLiteConnection(url: url)
        }
        dictionary = try open("dictionary_v2"); topicsDB = try open("topics")
        strongs = try open("strongs"); referencesDB = try open("cross_references_votes")
    }
    private func text(_ row: OpaquePointer, _ index: Int32) -> String {
        sqlite3_column_text(row, index).map { String(cString: $0) } ?? ""
    }
    private func load() throws {
        if dictionaryEntries.isEmpty {
            dictionaryEntries = try dictionary.rows("SELECT id, term, category, definition, display_title, gender, birth_year, death_year, latitude, longitude, aliases, feature_type FROM dictionary_entries ORDER BY term") { row in
                let labels = ["", "Género", "Nacimiento", "Fallecimiento", "Latitud", "Longitud", "Otros nombres", "Tipo de lugar"]
                var metadata: [String] = []
                for index in 4...11 {
                    var value = text(row, Int32(index))
                    if value.isEmpty || (index == 4 && value.searchKey == text(row, 1).searchKey) { continue }
                    if index == 5 { value = value.searchKey == "male" ? "Masculino" : value.searchKey == "female" ? "Femenino" : value }
                    if index == 6 || index == 7, let year = Int(value) { value = year <= 0 ? "\(year == 0 ? 1 : abs(year)) a.C." : "\(year) d.C." }
                    metadata.append(labels[index - 4].isEmpty ? value : "\(labels[index - 4]): \(value)")
                }
                return KnowledgeEntry(id: text(row, 0), title: text(row, 1), category: text(row, 2), definition: text(row, 3), metadata: metadata)
            }
        }
        if topicEntries.isEmpty {
            topicEntries = try topicsDB.rows("SELECT slug, name_es, category, description FROM topics ORDER BY verse_count DESC, name_es") { row in
                KnowledgeEntry(id: text(row, 0), title: text(row, 1), category: text(row, 2), definition: text(row, 3), metadata: [])
            }
        }
    }
    public func entries(query: String = "", category: String? = nil, topics: Bool = false) throws -> [KnowledgeEntry] {
        try load()
        let key = query.searchKey
        var entries = (topics ? topicEntries : dictionaryEntries).filter {
            (category == nil || $0.category == category) && (key.isEmpty || $0.title.searchKey.contains(key) || $0.metadata.joined(separator: " ").searchKey.contains(key))
        }
        if topics && !key.isEmpty {
            let aliases = try topicsDB.rows("SELECT DISTINCT topic_slug FROM topic_aliases WHERE low_confidence = 0 AND alias_en LIKE ?", values: ["%\(query)%"]) { text($0, 0) }
            let ids = Set(entries.map(\.id))
            entries += topicEntries.filter { !ids.contains($0.id) && aliases.contains($0.id) && (category == nil || $0.category == category) }
        }
        if category == "book" || category == "BOOK" {
            entries.sort { a, b in
                func index(_ title: String) -> Int { BibleBook.canonicalNames.firstIndex { $0.bookKey == title.bookKey } ?? 99 }
                return index(a.title) < index(b.title)
            }
        }
        return entries
    }
    public func exact(_ term: String) throws -> KnowledgeEntry? {
        try load()
        return dictionaryEntries.first { $0.title.searchKey == term.searchKey }
    }
    public func bookDescription(_ book: String) throws -> KnowledgeEntry? {
        try load()
        return topicEntries.first { $0.category == "BOOK" && $0.title.bookKey == book.bookKey }
    }
    public func topicReferences(_ slug: String) throws -> [TopicReference] {
        try topicsDB.rows("SELECT book, chapter, verse_start, verse_end FROM topic_references WHERE topic_slug = ? AND score >= 5 ORDER BY score DESC LIMIT 60", values: [slug]) {
            TopicReference(book: text($0, 0), chapter: Int(sqlite3_column_int($0, 1)), start: Int(sqlite3_column_int($0, 2)), end: Int(sqlite3_column_int($0, 3)))
        }
    }
    public func children(_ slug: String) throws -> [KnowledgeEntry] {
        try load()
        let ids = try topicsDB.rows("SELECT child_slug FROM topic_relationships WHERE parent_slug = ?", values: [slug]) { text($0, 0) }
        return topicEntries.filter { ids.contains($0.id) }
    }
    public func crossReferences(_ location: ReadingLocation) throws -> [String] {
        try referencesDB.rows("SELECT target_references FROM cross_reference_votes WHERE source_normalized_book = ? AND source_chapter = ? AND source_verse = ? AND votes >= 10 ORDER BY votes DESC LIMIT 20", values: [BibleBook.displayName(for: location.book).bookKey, String(location.chapter), String(location.verse ?? 1)]) { text($0, 0) }
    }
    public func strong(_ number: String) throws -> KnowledgeEntry? {
        try strongs.rows("SELECT strongs_number, lemma, transliteration, pronunciation, definition, derivation, kjv_renderings FROM strongs_entries WHERE strongs_number = ?", values: [number.uppercased()]) { row in
            KnowledgeEntry(id: text(row, 0), title: "\(text(row, 0)) · \(text(row, 1))", category: "Strong", definition: text(row, 4), metadata: [text(row, 2), text(row, 3), text(row, 5), text(row, 6)].filter { !$0.isEmpty })
        }.first
    }
}
