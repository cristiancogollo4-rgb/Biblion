import Foundation
import CSQLite

public enum BibleError: Error, LocalizedError, Sendable {
    case missingDatabase, database(String), unavailableChapter
    public var errorDescription: String? {
        switch self {
        case .missingDatabase: "No se encontró la Biblia local. Reinstala Biblion para recuperar sus textos."
        case .database: "No se pudo leer la Biblia local. Inténtalo de nuevo."
        case .unavailableChapter: "Este capítulo no está disponible en la versión seleccionada. Prueba otra versión."
        }
    }
}

// The connection never escapes BibleDatabase's actor; statements are always finalized.
final class SQLiteConnection {
    let handle: OpaquePointer
    init(url: URL) throws {
        var pointer: OpaquePointer?
        let result = sqlite3_open_v2(url.path, &pointer, SQLITE_OPEN_READONLY | SQLITE_OPEN_FULLMUTEX, nil)
        guard result == SQLITE_OK, let opened = pointer else {
            let message = pointer.map { String(cString: sqlite3_errmsg($0)) } ?? "Missing SQLite handle"
            if let pointer { sqlite3_close(pointer) }
            throw BibleError.database(message)
        }
        handle = opened
        sqlite3_create_function_v2(opened, "biblion_fold", 1, SQLITE_UTF8 | SQLITE_DETERMINISTIC, nil, { context, count, arguments in
            guard count == 1, let argument = arguments?[0], let bytes = sqlite3_value_text(argument) else { sqlite3_result_null(context); return }
            let value = String(cString: bytes).searchKey
            value.withCString { sqlite3_result_text(context, $0, -1, unsafeBitCast(-1, to: sqlite3_destructor_type.self)) }
        }, nil, nil, nil)
    }
    deinit { sqlite3_close(handle) }
    func rows<T>(_ sql: String, values: [String] = [], map: (OpaquePointer) -> T) throws -> [T] {
        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(handle, sql, -1, &statement, nil) == SQLITE_OK, let statement else {
            throw BibleError.database(String(cString: sqlite3_errmsg(handle)))
        }
        defer { sqlite3_finalize(statement) }
        let transient = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
        for (index, value) in values.enumerated() {
            guard sqlite3_bind_text(statement, Int32(index + 1), value, -1, transient) == SQLITE_OK else {
                throw BibleError.database("Unable to bind query")
            }
        }
        var rows: [T] = []
        while true {
            switch sqlite3_step(statement) {
            case SQLITE_ROW: rows.append(map(statement))
            case SQLITE_DONE: return rows
            default: throw BibleError.database(String(cString: sqlite3_errmsg(handle)))
            }
        }
    }
}

public actor BibleDatabase {
    private let connection: SQLiteConnection

    public func search(_ query: String, version: String, testament: Int? = nil, book: Int? = nil, offset: Int = 0) throws -> [VerseResult] {
        let words = query.trimmingCharacters(in: .whitespacesAndNewlines).split(whereSeparator: { $0.isWhitespace })
        guard !words.isEmpty else { return [] }
        var sql = "SELECT bookIndex, chapter, verse, text FROM bible_verses WHERE versionKey = ?"
        var values = [version]
        if let book { sql += " AND bookIndex = ?"; values.append(String(book)) }
        else if let testament { sql += testament == 0 ? " AND bookIndex < 39" : " AND bookIndex >= 39" }
        for word in words.prefix(12) {
            sql += " AND biblion_fold(text) LIKE ? ESCAPE '\\'"
            values.append("%" + String(word).searchKey.replacingOccurrences(of: "\\", with: "\\\\").replacingOccurrences(of: "%", with: "\\%").replacingOccurrences(of: "_", with: "\\_") + "%")
        }
        sql += " ORDER BY bookIndex, chapter, verse LIMIT 60 OFFSET ?"; values.append(String(max(0, offset)))
        return try connection.rows(sql, values: values) { row in
            VerseResult(location: ReadingLocation(version: version, book: Int(sqlite3_column_int(row, 0)), chapter: Int(sqlite3_column_int(row, 1)), verse: Int(sqlite3_column_int(row, 2))), text: String(cString: sqlite3_column_text(row, 3)))
        }
    }
    public init(url: URL? = nil) throws {
        guard let source = url ?? Bundle.module.url(forResource: "bible_content", withExtension: "db") else {
            throw BibleError.missingDatabase
        }
        connection = try SQLiteConnection(url: source)
    }

    public func versions() throws -> [BibleVersion] {
        let versions = try connection.rows("SELECT versionKey, COUNT(DISTINCT bookIndex) FROM bible_verses GROUP BY versionKey") {
            BibleVersion(id: String(cString: sqlite3_column_text($0, 0)), bookCount: Int(sqlite3_column_int($0, 1)))
        }
        let order = ["rv1960", "nvi", "dhh", "ntv", "tla", "pdt"]
        return versions.sorted { (order.firstIndex(of: $0.id) ?? 99) < (order.firstIndex(of: $1.id) ?? 99) }
    }

    public func books(version: String) throws -> [BibleBook] {
        let rows = try connection.rows("""
            SELECT bookIndex, chapter FROM bible_verses WHERE versionKey = ?
            GROUP BY bookIndex, chapter ORDER BY bookIndex, chapter
            """, values: [version]) { (Int(sqlite3_column_int($0, 0)), Int(sqlite3_column_int($0, 1))) }
        let groups = Dictionary(grouping: rows, by: { $0.0 })
        return groups.keys.sorted().map { index in
            BibleBook(id: index, name: BibleBook.displayName(for: index), chapters: groups[index, default: []].map(\.1).sorted())
        }
    }

    public func chapter(at location: ReadingLocation) throws -> BibleChapter {
        let verses = try connection.rows("""
            SELECT v.verse, v.text, t.title FROM bible_verses v
            LEFT JOIN bible_titles t ON t.versionKey = v.versionKey
              AND t.normalizedBookName = v.normalizedBookName AND t.chapter = v.chapter AND t.verse = v.verse
            WHERE v.versionKey = ? AND v.bookIndex = ? AND v.chapter = ? ORDER BY v.verse
            """, values: [location.version, String(location.book), String(location.chapter)]) { row in
            BibleVerse(number: Int(sqlite3_column_int(row, 0)), text: String(cString: sqlite3_column_text(row, 1)),
                       title: sqlite3_column_text(row, 2).map { String(cString: $0) })
        }
        guard !verses.isEmpty else { throw BibleError.unavailableChapter }
        return BibleChapter(location: location, verses: verses)
    }
}
