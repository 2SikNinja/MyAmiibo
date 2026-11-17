import Foundation

public struct SeriesProgress: Identifiable, Equatable, Sendable {
    public var id: String { series }
    public let series: String
    public let owned: Int
    public let total: Int

    public var percent: Int {
        total == 0 ? 0 : Int((Double(owned) / Double(total) * 100).rounded())
    }

    public var fraction: Double {
        total == 0 ? 0 : Double(owned) / Double(total)
    }
}

public struct CollectionStats: Equatable, Sendable {
    public let ownedCount: Int
    public let wishlistCount: Int
    public let totalCount: Int
    /// Only series where the user owns at least one amiibo, most complete first.
    public let series: [SeriesProgress]

    public var percent: Int {
        totalCount == 0 ? 0 : Int((Double(ownedCount) / Double(totalCount) * 100).rounded())
    }

    public var completedSeriesCount: Int {
        series.filter { $0.owned == $0.total }.count
    }

    public init(catalog: [Amiibo], collection: AmiiboCollection) {
        let ownedIds = Set(collection.owned)
        let knownIds = Set(catalog.map(\.id))
        var totals: [String: (owned: Int, total: Int)] = [:]

        for amiibo in catalog {
            var entry = totals[amiibo.amiiboSeries] ?? (owned: 0, total: 0)
            entry.total += 1
            if ownedIds.contains(amiibo.id) { entry.owned += 1 }
            totals[amiibo.amiiboSeries] = entry
        }

        series = totals
            .map { SeriesProgress(series: $0.key, owned: $0.value.owned, total: $0.value.total) }
            .filter { $0.owned > 0 }
            .sorted { a, b in
                if a.percent != b.percent { return a.percent > b.percent }
                if a.owned != b.owned { return a.owned > b.owned }
                return a.series.localizedStandardCompare(b.series) == .orderedAscending
            }

        ownedCount = collection.owned.filter { knownIds.contains($0) }.count
        wishlistCount = collection.wishlist.filter { knownIds.contains($0) }.count
        totalCount = catalog.count
    }
}
