import Foundation

public enum OwnershipFilter: String, CaseIterable, Identifiable, Sendable {
    case all
    case owned
    case wishlist
    case missing

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .all: return "All"
        case .owned: return "Owned"
        case .wishlist: return "Wishlist"
        case .missing: return "Not owned"
        }
    }
}

public enum SortOrder: String, CaseIterable, Identifiable, Sendable {
    case name
    case newest
    case oldest

    public var id: String { rawValue }

    public var label: String {
        switch self {
        case .name: return "Name (A to Z)"
        case .newest: return "Newest release"
        case .oldest: return "Oldest release"
        }
    }
}

public struct AmiiboFilters: Equatable, Sendable {
    public var query: String
    /// nil means any value.
    public var type: String?
    public var amiiboSeries: String?
    public var gameSeries: String?
    public var ownership: OwnershipFilter
    public var sort: SortOrder

    public init(
        query: String = "",
        type: String? = "Figure",
        amiiboSeries: String? = nil,
        gameSeries: String? = nil,
        ownership: OwnershipFilter = .all,
        sort: SortOrder = .name
    ) {
        self.query = query
        self.type = type
        self.amiiboSeries = amiiboSeries
        self.gameSeries = gameSeries
        self.ownership = ownership
        self.sort = sort
    }

    public static let defaults = AmiiboFilters()

    /// True when anything other than search text or sort differs from the defaults.
    public var hasActiveFilters: Bool {
        type != Self.defaults.type
            || amiiboSeries != nil
            || gameSeries != nil
            || ownership != .all
    }
}

public enum AmiiboSearch {
    static func normalize(_ text: String) -> String {
        text.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: nil)
            .lowercased()
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    public static func uniqueValues(_ list: [Amiibo], _ keyPath: KeyPath<Amiibo, String>) -> [String] {
        Array(Set(list.map { $0[keyPath: keyPath] }))
            .sorted { $0.localizedStandardCompare($1) == .orderedAscending }
    }

    public static func filter(
        _ list: [Amiibo],
        filters: AmiiboFilters,
        collection: AmiiboCollection
    ) -> [Amiibo] {
        let terms = normalize(filters.query)
            .split(whereSeparator: \.isWhitespace)
            .map(String.init)
        let ownedIds = Set(collection.owned)
        let wishlistIds = Set(collection.wishlist)

        let matches = list.filter { amiibo in
            if let type = filters.type, amiibo.type != type { return false }
            if let series = filters.amiiboSeries, amiibo.amiiboSeries != series { return false }
            if let series = filters.gameSeries, amiibo.gameSeries != series { return false }

            switch filters.ownership {
            case .all: break
            case .owned: if !ownedIds.contains(amiibo.id) { return false }
            case .wishlist: if !wishlistIds.contains(amiibo.id) { return false }
            case .missing: if ownedIds.contains(amiibo.id) { return false }
            }

            if !terms.isEmpty {
                let haystack = normalize(
                    "\(amiibo.name) \(amiibo.character) \(amiibo.amiiboSeries) \(amiibo.gameSeries)"
                )
                if !terms.allSatisfy({ haystack.contains($0) }) { return false }
            }
            return true
        }

        return sort(matches, by: filters.sort)
    }

    public static func sort(_ list: [Amiibo], by order: SortOrder) -> [Amiibo] {
        func byName(_ a: Amiibo, _ b: Amiibo) -> Bool {
            a.name.localizedStandardCompare(b.name) == .orderedAscending
        }

        switch order {
        case .name:
            return list.sorted(by: byName)
        case .newest, .oldest:
            let newestFirst = order == .newest
            return list.sorted { a, b in
                let dateA = a.release.earliest
                let dateB = b.release.earliest
                if dateA == dateB { return byName(a, b) }
                // Unreleased or unknown dates always go last.
                guard let dateA else { return false }
                guard let dateB else { return true }
                return newestFirst ? dateA > dateB : dateA < dateB
            }
        }
    }
}
