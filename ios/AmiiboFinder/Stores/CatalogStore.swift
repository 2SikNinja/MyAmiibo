import AmiiboCore
import Foundation
import Observation

/// Loads the amiibo catalog. The raw API response is cached on disk so the app
/// opens instantly and works offline; it refreshes in the background once a day.
@MainActor
@Observable
final class CatalogStore {
    enum LoadState: Equatable {
        case idle
        case loading
        case loaded
        case failed(String)
    }

    private(set) var catalog: [Amiibo] = []
    private(set) var state: LoadState = .idle
    private(set) var lastUpdated: Date?

    private let client: AmiiboAPIClient
    private let cacheURL: URL
    private let refreshInterval: TimeInterval = 60 * 60 * 24
    private var detailCache: [String: AmiiboDetail] = [:]

    init(client: AmiiboAPIClient = AmiiboAPIClient()) {
        self.client = client
        let cachesDirectory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        cacheURL = cachesDirectory.appendingPathComponent("amiibo-catalog.json")
    }

    func amiibo(withId id: String) -> Amiibo? {
        catalog.first { $0.id == id }
    }

    /// Shows the cached catalog right away, then refreshes when it's stale.
    func loadIfNeeded() async {
        guard state == .idle else { return }
        loadFromDisk()
        let isStale = lastUpdated.map { Date().timeIntervalSince($0) > refreshInterval } ?? true
        if catalog.isEmpty || isStale {
            await refresh()
        } else {
            state = .loaded
        }
    }

    func refresh() async {
        if catalog.isEmpty { state = .loading }
        do {
            let data = try await client.fetchCatalogData()
            catalog = sortedByName(try AmiiboDecoding.decodeList(data))
            try? data.write(to: cacheURL, options: .atomic)
            lastUpdated = Date()
            state = .loaded
        } catch {
            // Keep showing cached data if there is any. Only surface the
            // error when there's nothing to show.
            state = catalog.isEmpty ? .failed(error.localizedDescription) : .loaded
        }
    }

    func detail(for id: String) async throws -> AmiiboDetail? {
        if let cached = detailCache[id] { return cached }
        let detail = try await client.fetchDetail(id: id)
        if let detail { detailCache[id] = detail }
        return detail
    }

    private func loadFromDisk() {
        guard let data = try? Data(contentsOf: cacheURL),
              let list = try? AmiiboDecoding.decodeList(data),
              !list.isEmpty else {
            return
        }
        catalog = sortedByName(list)
        let attributes = try? FileManager.default.attributesOfItem(atPath: cacheURL.path)
        lastUpdated = attributes?[.modificationDate] as? Date
    }

    private func sortedByName(_ list: [Amiibo]) -> [Amiibo] {
        AmiiboSearch.sort(list, by: .name)
    }
}
