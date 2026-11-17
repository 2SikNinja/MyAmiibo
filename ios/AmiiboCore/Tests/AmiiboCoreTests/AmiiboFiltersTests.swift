import XCTest
@testable import AmiiboCore

final class AmiiboFiltersTests: XCTestCase {
    private var catalog: [Amiibo] = []
    private let anyType = AmiiboFilters(type: nil)

    override func setUpWithError() throws {
        catalog = try AmiiboDecoding.decodeList(Fixtures.listData)
    }

    private func names(_ filters: AmiiboFilters, _ collection: AmiiboCollection = .empty) -> [String] {
        AmiiboSearch.filter(catalog, filters: filters, collection: collection).map(\.name)
    }

    func testDefaultsToFiguresOnly() {
        XCTAssertEqual(names(.defaults), ["Isabelle - Winter Outfit", "Mario"])
    }

    func testSearchIgnoresCaseAndAccents() {
        var filters = anyType
        filters.query = "CELESTE"
        XCTAssertEqual(names(filters), ["Céleste"])
        filters.query = "smash mario"
        XCTAssertEqual(names(filters), ["Mario"])
        filters.query = "pokemon trainer"
        XCTAssertEqual(names(filters), ["Céleste"])
    }

    func testSeriesFilters() {
        var filters = anyType
        filters.amiiboSeries = "Animal Crossing"
        XCTAssertEqual(names(filters).count, 2)
        filters = anyType
        filters.gameSeries = "Super Mario"
        XCTAssertEqual(names(filters), ["Mario"])
    }

    func testOwnershipFilters() {
        let collection = AmiiboCollection(owned: [Fixtures.marioId], wishlist: [Fixtures.cardId])
        var filters = anyType
        filters.ownership = .owned
        XCTAssertEqual(names(filters, collection), ["Mario"])
        filters.ownership = .wishlist
        XCTAssertEqual(names(filters, collection), ["Céleste"])
        filters.ownership = .missing
        XCTAssertEqual(names(filters, collection), ["Céleste", "Isabelle - Winter Outfit"])
    }

    func testReleaseSortPutsUnreleasedLast() {
        var filters = anyType
        filters.sort = .newest
        XCTAssertEqual(names(filters), ["Isabelle - Winter Outfit", "Mario", "Céleste"])
        filters.sort = .oldest
        XCTAssertEqual(names(filters), ["Mario", "Isabelle - Winter Outfit", "Céleste"])
    }

    func testUniqueValues() {
        XCTAssertEqual(AmiiboSearch.uniqueValues(catalog, \.type), ["Card", "Figure"])
    }

    func testHasActiveFilters() {
        XCTAssertFalse(AmiiboFilters.defaults.hasActiveFilters)
        XCTAssertTrue(anyType.hasActiveFilters)
    }
}
