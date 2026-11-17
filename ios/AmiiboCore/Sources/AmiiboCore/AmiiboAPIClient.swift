import Foundation
#if canImport(FoundationNetworking)
import FoundationNetworking
#endif

public struct AmiiboAPIClient: Sendable {
    public enum APIError: Error, LocalizedError {
        case badStatus(Int)
        case emptyCatalog

        public var errorDescription: String? {
            switch self {
            case .badStatus(let code):
                return "AmiiboAPI responded with status \(code)."
            case .emptyCatalog:
                return "AmiiboAPI returned no amiibo."
            }
        }
    }

    public let baseURL: URL
    private let session: URLSession

    public init(
        baseURL: URL = URL(string: "https://www.amiiboapi.com/api")!,
        session: URLSession = .shared
    ) {
        self.baseURL = baseURL
        self.session = session
    }

    /// Raw catalog JSON, so callers can cache it on disk as-is.
    public func fetchCatalogData() async throws -> Data {
        let url = baseURL.appendingPathComponent("amiibo/")
        let data = try await get(url)
        guard try !AmiiboDecoding.decodeList(data).isEmpty else {
            throw APIError.emptyCatalog
        }
        return data
    }

    /// One amiibo with game usage, or nil when the id is unknown.
    public func fetchDetail(id: String) async throws -> AmiiboDetail? {
        guard AmiiboCollection.isValidId(id) else { return nil }
        var components = URLComponents(
            url: baseURL.appendingPathComponent("amiibo/"),
            resolvingAgainstBaseURL: false
        )!
        components.queryItems = [
            URLQueryItem(name: "id", value: id),
            URLQueryItem(name: "showusage", value: nil),
        ]
        do {
            return try AmiiboDecoding.decodeDetail(try await get(components.url!))
        } catch APIError.badStatus(let code) where code == 404 {
            return nil
        }
    }

    private func get(_ url: URL) async throws -> Data {
        var request = URLRequest(url: url)
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.timeoutInterval = 30
        let (data, response) = try await session.data(for: request)
        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw APIError.badStatus(http.statusCode)
        }
        return data
    }
}
