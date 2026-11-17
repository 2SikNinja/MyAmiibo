import Foundation

/// The user's owned and wishlist amiibo ids. Owned and wishlist are exclusive.
public struct AmiiboCollection: Codable, Equatable, Sendable {
    public private(set) var owned: [String]
    public private(set) var wishlist: [String]

    public static let empty = AmiiboCollection(owned: [], wishlist: [])

    public init(owned: [String], wishlist: [String]) {
        let cleanOwned = Self.cleanIds(owned)
        let ownedSet = Set(cleanOwned)
        self.owned = cleanOwned
        self.wishlist = Self.cleanIds(wishlist).filter { !ownedSet.contains($0) }
    }

    public init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        let owned = (try? container.decodeIfPresent([String].self, forKey: .owned)) ?? nil
        let wishlist = (try? container.decodeIfPresent([String].self, forKey: .wishlist)) ?? nil
        self.init(owned: owned ?? [], wishlist: wishlist ?? [])
    }

    /// Parses stored or imported JSON. Never throws; bad input gives an empty collection.
    /// The format matches the web app's backup file, so backups move between them.
    public static func parse(_ data: Data?) -> AmiiboCollection {
        guard let data, let collection = try? JSONDecoder().decode(AmiiboCollection.self, from: data) else {
            return .empty
        }
        return collection
    }

    public func encoded() -> Data {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return (try? encoder.encode(self)) ?? Data()
    }

    public var isEmpty: Bool {
        owned.isEmpty && wishlist.isEmpty
    }

    public func status(of id: String) -> CollectionStatus? {
        if owned.contains(id) { return .owned }
        if wishlist.contains(id) { return .wishlist }
        return nil
    }

    /// Sets a status. Passing nil removes the amiibo from both lists.
    public func setting(_ status: CollectionStatus?, for id: String) -> AmiiboCollection {
        var newOwned = owned.filter { $0 != id }
        var newWishlist = wishlist.filter { $0 != id }
        switch status {
        case .owned: newOwned.append(id)
        case .wishlist: newWishlist.append(id)
        case nil: break
        }
        return AmiiboCollection(owned: newOwned, wishlist: newWishlist)
    }

    /// Tapping the active status clears it; tapping the other one switches.
    public func toggling(_ status: CollectionStatus, for id: String) -> AmiiboCollection {
        setting(self.status(of: id) == status ? nil : status, for: id)
    }

    static func cleanIds(_ ids: [String]) -> [String] {
        var seen = Set<String>()
        return ids.filter { id in
            guard isValidId(id), !seen.contains(id) else { return false }
            seen.insert(id)
            return true
        }
    }

    static func isValidId(_ id: String) -> Bool {
        id.count == 16 && id.allSatisfy(\.isHexDigit)
    }
}
