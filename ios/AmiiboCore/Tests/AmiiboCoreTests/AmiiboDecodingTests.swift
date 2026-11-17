import XCTest
@testable import AmiiboCore

final class AmiiboDecodingTests: XCTestCase {
    func testListSkipsDuplicatesAndInvalidEntries() throws {
        let list = try AmiiboDecoding.decodeList(Fixtures.listData)
        XCTAssertEqual(list.map(\.name), ["Mario", "Isabelle - Winter Outfit", "Céleste"])
        XCTAssertEqual(list.first?.id, Fixtures.marioId)
    }

    func testReleaseDatesKeepNulls() throws {
        let list = try AmiiboDecoding.decodeList(Fixtures.listData)
        let isabelle = try XCTUnwrap(list.first { $0.id == Fixtures.isabelleId })
        XCTAssertEqual(isabelle.release, ReleaseDates(na: "2015-11-13", eu: "2016-03-18"))
        XCTAssertEqual(isabelle.release.earliest, "2015-11-13")
    }

    func testDetailOrdersPlatformsAndSkipsEmptyOnes() throws {
        let detail = try XCTUnwrap(try AmiiboDecoding.decodeDetail(Fixtures.marioDetailData))
        XCTAssertEqual(detail.usage.map(\.platform), ["Nintendo Switch", "Nintendo 3DS"])
        XCTAssertEqual(detail.gameCount, 3)
    }

    func testDetailSortsGamesAndMapsUsage() throws {
        let detail = try XCTUnwrap(try AmiiboDecoding.decodeDetail(Fixtures.marioDetailData))
        let switchGames = detail.usage[0].games
        XCTAssertEqual(switchGames.map(\.gameName), ["Super Mario Odyssey", "Super Smash Bros. Ultimate"])
        XCTAssertEqual(switchGames[1].usages, [UsageEntry(description: "Battle a figure player", writesData: true)])
    }

    func testDetailAcceptsArrayEnvelope() throws {
        let data = Data("{ \"amiibo\": [\(Fixtures.marioJSON)] }".utf8)
        XCTAssertEqual(try AmiiboDecoding.decodeDetail(data)?.amiibo.name, "Mario")
    }

    func testMissingAmiiboKeyGivesEmptyList() throws {
        XCTAssertEqual(try AmiiboDecoding.decodeList(Data(#"{"code":404}"#.utf8)), [])
    }
}
