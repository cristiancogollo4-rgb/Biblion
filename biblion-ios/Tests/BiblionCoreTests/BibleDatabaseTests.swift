import XCTest
@testable import BiblionCore

@MainActor
final class BibleDatabaseTests: XCTestCase {
    func testBundledVersionsAndCanonicalCatalog() async throws {
        let database = try BibleDatabase()
        let versions = try await database.versions()
        XCTAssertEqual(versions.first?.id, "rv1960")
        XCTAssertEqual(Set(versions.map(\.id)), ["rv1960", "nvi", "dhh", "ntv", "tla", "pdt"])
        XCTAssertEqual(versions.first { $0.id == "pdt" }?.bookCount, 49)
        let books = try await database.books(version: "rv1960")
        XCTAssertEqual(books.count, 66)
        XCTAssertEqual(books.filter { Testament.old.contains($0) }.count, 39)
        XCTAssertEqual(books.filter { Testament.new.contains($0) }.count, 27)
        XCTAssertEqual(books.first?.name, "Génesis")
        XCTAssertEqual(books.first?.chapters, Array(1...50))
        XCTAssertEqual(books.last?.name, "Apocalipsis")
        XCTAssertEqual(books.last?.chapters, Array(1...22))
    }

    func testReadsRealTextAndSectionTitles() async throws {
        let database = try BibleDatabase()
        let chapter = try await database.chapter(at: ReadingLocation())
        XCTAssertEqual(chapter.verses.count, 31)
        XCTAssertTrue(chapter.verses[0].text.hasPrefix("En el principio creó Dios"))
        XCTAssertEqual(chapter.verses[0].number, 1)
        XCTAssertNotNil(chapter.verses[0].title)
        XCTAssertEqual(chapter.verses.last?.number, 31)
    }

    func testVersionSpecificBookNamesDoNotBreakLookup() async throws {
        let database = try BibleDatabase()
        for key in ["rv1960", "ntv", "nvi"] {
            for index in [21, 43] {
                let chapter = try await database.chapter(at: ReadingLocation(version: key, book: index))
                XCTAssertFalse(chapter.verses.isEmpty, "\(key), book \(index)")
                XCTAssertEqual(chapter.verses.first?.number, 1)
            }
        }
    }

    func testInvalidChapterAndUntrustedVersionDoNotReturnUnrelatedText() async throws {
        let database = try BibleDatabase()
        for location in [ReadingLocation(chapter: 999), ReadingLocation(version: "' OR 1=1 --")] {
            do {
                _ = try await database.chapter(at: location)
                XCTFail("Expected an unavailable chapter error")
            } catch BibleError.unavailableChapter {} catch { XCTFail("Unexpected error: \(error)") }
        }
    }

    func testConcurrentReadsRemainInTheirRequestedChapter() async throws {
        let database = try BibleDatabase()
        let numbers = try await withThrowingTaskGroup(of: Int.self) { group in
            for chapter in 1...12 {
                group.addTask {
                    let result = try await database.chapter(at: ReadingLocation(book: 0, chapter: chapter))
                    guard !result.verses.isEmpty else { throw BibleError.unavailableChapter }
                    return result.location.chapter
                }
            }
            var completed: [Int] = []
            for try await result in group { completed.append(result) }
            return completed.sorted()
        }
        XCTAssertEqual(numbers, Array(1...12))
    }

    func testChapterNavigationCrossesBooksAndTestamentsAndStopsAtEdges() async throws {
        let books = try await BibleDatabase().books(version: "rv1960")
        XCTAssertNil(ChapterNavigation.adjacent(to: ReadingLocation(), in: books, forward: false))
        XCTAssertNil(ChapterNavigation.adjacent(to: ReadingLocation(book: 65, chapter: 22), in: books, forward: true))
        XCTAssertEqual(ChapterNavigation.adjacent(to: ReadingLocation(book: 0, chapter: 50), in: books, forward: true),
                       ReadingLocation(book: 1, chapter: 1))
        XCTAssertEqual(ChapterNavigation.adjacent(to: ReadingLocation(book: 39, chapter: 1), in: books, forward: false),
                       ReadingLocation(book: 38, chapter: 4))
        XCTAssertNil(ChapterNavigation.adjacent(to: ReadingLocation(chapter: 999), in: books, forward: true))
    }

    func testPartialVersionOnlyNavigatesAvailableContent() async throws {
        let books = try await BibleDatabase().books(version: "pdt")
        XCTAssertEqual(books.count, 49)
        for (index, book) in books.dropLast().enumerated() {
            let location = ReadingLocation(version: "pdt", book: book.id, chapter: try XCTUnwrap(book.chapters.last))
            let next = ChapterNavigation.adjacent(to: location, in: books, forward: true)
            XCTAssertEqual(next?.book, books[index + 1].id)
            XCTAssertEqual(next?.chapter, books[index + 1].chapters.first)
        }
    }
}
