import Foundation

/// Turns raw AmiiboAPI JSON into app models. Bad entries are skipped rather
/// than failing the whole response.
public enum AmiiboDecoding {
    public enum DecodingFailure: Error {
        case emptyCatalog
    }

    public static func decodeList(_ data: Data) throws -> [Amiibo] {
        let envelope = try JSONDecoder().decode(Envelope.self, from: data)
        var seenIds = Set<String>()
        var result: [Amiibo] = []
        for raw in envelope.amiibo {
            guard let amiibo = makeAmiibo(raw), !seenIds.contains(amiibo.id) else { continue }
            seenIds.insert(amiibo.id)
            result.append(amiibo)
        }
        return result
    }

    /// The `?id=` endpoint returns a single object, but tolerate an array too.
    public static func decodeDetail(_ data: Data) throws -> AmiiboDetail? {
        let envelope = try JSONDecoder().decode(Envelope.self, from: data)
        guard let raw = envelope.amiibo.first, let amiibo = makeAmiibo(raw) else { return nil }
        return AmiiboDetail(amiibo: amiibo, usage: makeUsage(raw.platforms))
    }

    // MARK: - Normalizing

    static let platformLabels: [String: String] = [
        "games3DS": "Nintendo 3DS",
        "gamesWiiU": "Wii U",
        "gamesSwitch": "Nintendo Switch",
        "gamesSwitch2": "Nintendo Switch 2",
    ]

    static let platformOrder = ["gamesSwitch2", "gamesSwitch", "gamesWiiU", "games3DS"]

    static func makeAmiibo(_ raw: RawAmiibo) -> Amiibo? {
        guard let head = raw.head, let tail = raw.tail, let name = raw.name,
              !head.isEmpty, !tail.isEmpty, !name.isEmpty else {
            return nil
        }
        return Amiibo(
            id: head + tail,
            name: name,
            character: raw.character ?? name,
            amiiboSeries: raw.amiiboSeries ?? "Unknown",
            gameSeries: raw.gameSeries ?? "Unknown",
            type: raw.type ?? "Unknown",
            imageURL: raw.image.flatMap { URL(string: $0) },
            release: ReleaseDates(
                na: raw.release?.na,
                eu: raw.release?.eu,
                jp: raw.release?.jp,
                au: raw.release?.au
            )
        )
    }

    static func makeUsage(_ platforms: [String: [RawGame]]) -> [PlatformUsage] {
        func rank(_ key: String) -> Int {
            platformOrder.firstIndex(of: key) ?? 99
        }

        return platforms.keys
            .sorted { rank($0) == rank($1) ? $0 < $1 : rank($0) < rank($1) }
            .compactMap { key -> PlatformUsage? in
                let games = (platforms[key] ?? [])
                    .compactMap { game -> GameUsage? in
                        guard let gameName = game.gameName else { return nil }
                        let usages = (game.amiiboUsage ?? []).compactMap { usage -> UsageEntry? in
                            let text = (usage.usage ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
                            guard !text.isEmpty else { return nil }
                            return UsageEntry(description: text, writesData: usage.write ?? false)
                        }
                        return GameUsage(gameName: gameName, usages: usages)
                    }
                    .sorted { $0.gameName.localizedStandardCompare($1.gameName) == .orderedAscending }
                guard !games.isEmpty else { return nil }
                let label = platformLabels[key] ?? String(key.dropFirst("games".count))
                return PlatformUsage(platform: label, games: games)
            }
    }
}

// MARK: - Raw API shapes

struct DynamicKey: CodingKey {
    let stringValue: String
    let intValue: Int? = nil

    init(_ string: String) {
        stringValue = string
    }

    init?(stringValue: String) {
        self.stringValue = stringValue
    }

    init?(intValue: Int) {
        return nil
    }
}

struct Envelope: Decodable {
    let amiibo: [RawAmiibo]

    enum CodingKeys: String, CodingKey {
        case amiibo
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        if let list = try? container.decode([RawAmiibo].self, forKey: .amiibo) {
            amiibo = list
        } else if let single = try? container.decode(RawAmiibo.self, forKey: .amiibo) {
            amiibo = [single]
        } else {
            amiibo = []
        }
    }
}

struct RawRelease: Decodable {
    let na: String?
    let eu: String?
    let jp: String?
    let au: String?
}

struct RawUsage: Decodable {
    let usage: String?
    let write: Bool?

    enum CodingKeys: String, CodingKey {
        case usage = "Usage"
        case write
    }
}

struct RawGame: Decodable {
    let gameName: String?
    let amiiboUsage: [RawUsage]?
}

struct RawAmiibo: Decodable {
    let head: String?
    let tail: String?
    let name: String?
    let character: String?
    let amiiboSeries: String?
    let gameSeries: String?
    let type: String?
    let image: String?
    let release: RawRelease?
    /// Every `games*` array, keyed by its API name (games3DS, gamesSwitch, ...).
    let platforms: [String: [RawGame]]

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: DynamicKey.self)

        func string(_ key: String) -> String? {
            (try? container.decodeIfPresent(String.self, forKey: DynamicKey(key))) ?? nil
        }

        head = string("head")
        tail = string("tail")
        name = string("name")
        character = string("character")
        amiiboSeries = string("amiiboSeries")
        gameSeries = string("gameSeries")
        type = string("type")
        image = string("image")
        release = (try? container.decodeIfPresent(RawRelease.self, forKey: DynamicKey("release"))) ?? nil

        var platforms: [String: [RawGame]] = [:]
        for key in container.allKeys where key.stringValue.hasPrefix("games") {
            if let games = try? container.decode([RawGame].self, forKey: key) {
                platforms[key.stringValue] = games
            }
        }
        self.platforms = platforms
    }
}
