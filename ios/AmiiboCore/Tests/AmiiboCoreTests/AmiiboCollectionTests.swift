import XCTest
@testable import AmiiboCore

final class AmiiboCollectionTests: XCTestCase {
    func testParseHandlesBadInput() {
        XCTAssertEqual(AmiiboCollection.parse(nil), .empty)
        XCTAssertEqual(AmiiboCollection.parse(Data("not json".utf8)), .empty)
        XCTAssertEqual(AmiiboCollection.parse(Data("[1,2]".utf8)), .empty)
    }

    func testParseCleansIds() {
        let json = """
        { "owned": ["\(Fixtures.marioId)", "\(Fixtures.marioId)", "bad"],
          "wishlist": ["\(Fixtures.marioId)", "\(Fixtures.isabelleId)"] }
        """
        let collection = AmiiboCollection.parse(Data(json.utf8))
        XCTAssertEqual(collection.owned, [Fixtures.marioId])
        XCTAssertEqual(collection.wishlist, [Fixtures.isabelleId])
    }

    func testRoundTripMatchesWebBackupFormat() {
        let collection = AmiiboCollection(owned: [Fixtures.marioId], wishlist: [Fixtures.isabelleId])
        XCTAssertEqual(AmiiboCollection.parse(collection.encoded()), collection)
    }

    func testOwnedAndWishlistAreExclusive() {
        let wished = AmiiboCollection.empty.setting(.wishlist, for: Fixtures.marioId)
        let owned = wished.setting(.owned, for: Fixtures.marioId)
        XCTAssertEqual(owned.owned, [Fixtures.marioId])
        XCTAssertEqual(owned.wishlist, [])
        XCTAssertEqual(owned.status(of: Fixtures.marioId), .owned)
    }

    func testTogglingActiveStatusClearsIt() {
        let owned = AmiiboCollection.empty.toggling(.owned, for: Fixtures.marioId)
        XCTAssertEqual(owned.toggling(.owned, for: Fixtures.marioId), .empty)
    }

    func testStatsCountPerSeriesAndIgnoreUnknownIds() throws {
        let catalog = try AmiiboDecoding.decodeList(Fixtures.listData)
        let stats = CollectionStats(
            catalog: catalog,
            collection: AmiiboCollection(owned: [Fixtures.isabelleId, "ffffffffffffffff"], wishlist: [Fixtures.marioId])
        )
        XCTAssertEqual(stats.ownedCount, 1)
        XCTAssertEqual(stats.wishlistCount, 1)
        XCTAssertEqual(stats.percent, 33)
        XCTAssertEqual(stats.series, [SeriesProgress(series: "Animal Crossing", owned: 1, total: 2)])
        XCTAssertEqual(stats.completedSeriesCount, 0)
    }
}
