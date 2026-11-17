import Foundation

public struct ReleaseDates: Codable, Hashable, Sendable {
    public var na: String?
    public var eu: String?
    public var jp: String?
    public var au: String?

    public init(na: String? = nil, eu: String? = nil, jp: String? = nil, au: String? = nil) {
        self.na = na
        self.eu = eu
        self.jp = jp
        self.au = au
    }

    /// Regions in display order, paired with a readable label.
    public var byRegion: [RegionRelease] {
        [
            RegionRelease(region: "North America", date: na),
            RegionRelease(region: "Europe", date: eu),
            RegionRelease(region: "Japan", date: jp),
            RegionRelease(region: "Australia", date: au),
        ]
    }

    /// Earliest known release date across regions, as an ISO date string.
    public var earliest: String? {
        [na, eu, jp, au]
            .compactMap { $0 }
            .filter { !$0.isEmpty }
            .min()
    }
}

public struct RegionRelease: Identifiable, Hashable, Sendable {
    public var id: String { region }
    public let region: String
    public let date: String?
}

public struct Amiibo: Identifiable, Codable, Hashable, Sendable {
    /// head + tail from AmiiboAPI. Unique per amiibo.
    public let id: String
    public let name: String
    public let character: String
    public let amiiboSeries: String
    public let gameSeries: String
    public let type: String
    public let imageURL: URL?
    public let release: ReleaseDates

    public init(
        id: String,
        name: String,
        character: String,
        amiiboSeries: String,
        gameSeries: String,
        type: String,
        imageURL: URL?,
        release: ReleaseDates
    ) {
        self.id = id
        self.name = name
        self.character = character
        self.amiiboSeries = amiiboSeries
        self.gameSeries = gameSeries
        self.type = type
        self.imageURL = imageURL
        self.release = release
    }
}

public struct UsageEntry: Hashable, Sendable {
    public let description: String
    public let writesData: Bool

    public init(description: String, writesData: Bool) {
        self.description = description
        self.writesData = writesData
    }
}

public struct GameUsage: Identifiable, Hashable, Sendable {
    public var id: String { gameName }
    public let gameName: String
    public let usages: [UsageEntry]

    public init(gameName: String, usages: [UsageEntry]) {
        self.gameName = gameName
        self.usages = usages
    }
}

public struct PlatformUsage: Identifiable, Hashable, Sendable {
    public var id: String { platform }
    public let platform: String
    public let games: [GameUsage]

    public init(platform: String, games: [GameUsage]) {
        self.platform = platform
        self.games = games
    }
}

public struct AmiiboDetail: Hashable, Sendable {
    public let amiibo: Amiibo
    public let usage: [PlatformUsage]

    public init(amiibo: Amiibo, usage: [PlatformUsage]) {
        self.amiibo = amiibo
        self.usage = usage
    }

    public var gameCount: Int {
        usage.reduce(0) { $0 + $1.games.count }
    }
}

public enum CollectionStatus: String, Codable, CaseIterable, Sendable {
    case owned
    case wishlist
}
