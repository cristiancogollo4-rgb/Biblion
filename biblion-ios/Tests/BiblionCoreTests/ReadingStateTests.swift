import Foundation
import XCTest
@testable import BiblionCore

@MainActor
final class ReadingStateTests: XCTestCase {
    func testDefaultsAndFontBounds() {
        var state = ReadingState()
        XCTAssertEqual(state.selectedVersion, "rv1960")
        XCTAssertEqual(state.appearance, .light)
        state.fontSize = 100
        state.normalize()
        XCTAssertEqual(state.fontSize, 34)
        state.fontSize = -10
        state.normalize()
        XCTAssertEqual(state.fontSize, 16)
        state.fontSize = .nan
        state.normalize()
        XCTAssertEqual(state.fontSize, 20)
    }

    func testHighlightIdentityKeepsVersionsSeparateAndToggleRemoves() {
        var state = ReadingState()
        let original = ReadingLocation(book: 42, chapter: 3, verse: 16)
        var translation = original
        translation.version = "nvi"
        state.toggleHighlight(location: original, text: "Texto original")
        state.toggleHighlight(location: translation, text: "Otra traducción")
        XCTAssertEqual(state.highlights.count, 2)
        state.toggleHighlight(location: original, text: "Texto original")
        XCTAssertEqual(state.highlights.map(\.location), [translation])
        state.toggleHighlight(location: ReadingLocation(), text: "No es un versículo")
        XCTAssertEqual(state.highlights.count, 1)
    }

    func testStateSurvivesRelaunchIncludingExactReadingPosition() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: directory) }
        let url = directory.appendingPathComponent("reading.json")
        let store = ReadingStateStore(url: url)
        let initial = try await store.load()
        XCTAssertEqual(initial, ReadingState())
        var state = ReadingState()
        state.selectedVersion = "ntv"
        state.appearance = .dark
        state.fontSize = 26
        state.lastLocation = ReadingLocation(version: "ntv", book: 18, chapter: 119, verse: 105)
        state.toggleHighlight(location: try XCTUnwrap(state.lastLocation), text: "Texto guardado")
        try await store.save(state)
        let reloaded = try await ReadingStateStore(url: url).load()
        XCTAssertEqual(reloaded, state)
    }

    func testCorruptStateIsReportedAndNeverOverwrittenByLoad() async throws {
        let directory = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: directory) }
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let url = directory.appendingPathComponent("reading.json")
        let corrupted = Data("not-json".utf8)
        try corrupted.write(to: url)
        do {
            _ = try await ReadingStateStore(url: url).load()
            XCTFail("Corruption must not silently reset user data")
        } catch is DecodingError {} catch { XCTFail("Unexpected error: \(error)") }
        XCTAssertEqual(try Data(contentsOf: url), corrupted)
    }
}
