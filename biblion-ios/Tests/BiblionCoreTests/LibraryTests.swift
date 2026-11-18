import XCTest
@testable import BiblionCore

final class LibraryTests: XCTestCase, @unchecked Sendable {
    func testRealDictionaryCategoriesAndAccents() async throws {
        let library = try KnowledgeLibrary()
        let all = try await library.entries()
        XCTAssertEqual(all.count, 5758)
        let abraham = try await library.exact("Abrahám")
        XCTAssertNotNil(abraham)
        XCTAssertEqual(abraham?.category, "person")
        XCTAssertTrue(abraham?.metadata.contains("Género: Masculino") == true)
        let places = try await library.entries(category: "place")
        XCTAssertGreaterThan(places.count, 1000)
        XCTAssertTrue(places.allSatisfy { $0.category == "place" })
    }
    func testCanonicalTopicsReferencesAndStrong() async throws {
        let library = try KnowledgeLibrary()
        let books = try await library.entries(category: "BOOK", topics: true)
        XCTAssertEqual(books.count, 66)
        XCTAssertEqual(books.first?.title, "Génesis")
        XCTAssertEqual(books.last?.title, "Apocalipsis")
        let refs = try await library.topicReferences("genesis")
        XCTAssertFalse(refs.isEmpty)
        XCTAssertTrue(refs.contains { $0.location(version: "rv1960")?.book == 0 })
        let strong = try await library.strong("H1254")
        XCTAssertNotNil(strong)
        XCTAssertFalse(strong?.definition.isEmpty ?? true)
        let cross = try await library.crossReferences(ReadingLocation(version: "rv1960", book: 0, chapter: 1, verse: 1))
        XCTAssertTrue(cross.contains { $0.contains("Jeremias 10:12") })
    }
    func testSearchFiltersPaginationAndLiteralEscaping() async throws {
        let db = try BibleDatabase()
        let first = try await db.search("amor", version: "rv1960")
        let second = try await db.search("amor", version: "rv1960", offset: 60)
        XCTAssertEqual(first.count, 60)
        XCTAssertTrue(Set(first.map(\.id)).isDisjoint(with: Set(second.map(\.id))))
        let filtered = try await db.search("amor", version: "nvi", testament: 1, book: 42)
        XCTAssertFalse(filtered.isEmpty)
        XCTAssertTrue(filtered.allSatisfy { $0.location.book == 42 && $0.location.version == "nvi" })
        let literal = try await db.search("%' OR 1=1 --", version: "rv1960")
        XCTAssertTrue(literal.isEmpty)
        let wildcard = try await db.search("%", version: "rv1960")
        XCTAssertTrue(wildcard.isEmpty)
    }
    func testAssistantEnvelopeIsNeverDisplayed() throws {
        XCTAssertEqual(AssistantAnswer.clean(" Una respuesta bíblica. "), "Una respuesta bíblica.")
        XCTAssertEqual(AssistantAnswer.clean(#"{"answer":"Texto seguro","references":[]}"#), "Texto seguro")
        XCTAssertEqual(AssistantAnswer.clean("```json\n{\"answer\":\"Texto seguro\"}\n```"), "Texto seguro")
        XCTAssertNil(AssistantAnswer.clean(#"{"answer":"respuesta incompleta"#))
        XCTAssertNil(AssistantAnswer.clean(#"{"confidence":"high"}"#))
        XCTAssertNil(AssistantAnswer.clean("  "))
    }
    func testPreviousReadingFileDecodesAfterAddingSyncMetadata() throws {
        let data = Data(#"{"selectedVersion":"rv1960","appearance":"light","fontSize":20,"highlights":[]}"#.utf8)
        let state = try JSONDecoder().decode(ReadingState.self, from: data)
        XCTAssertNil(state.syncTimes)
        XCTAssertEqual(state.selectedVersion, "rv1960")
    }
}
