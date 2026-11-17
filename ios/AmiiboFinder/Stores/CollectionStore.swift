import AmiiboCore
import Foundation
import Observation

/// The user's owned and wishlist amiibo, saved on this device.
@MainActor
@Observable
final class CollectionStore {
    private(set) var collection: AmiiboCollection

    private let defaults: UserDefaults
    private let storageKey = "amiibo-collection-v1"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        collection = AmiiboCollection.parse(defaults.data(forKey: storageKey))
    }

    func status(of id: String) -> CollectionStatus? {
        collection.status(of: id)
    }

    func toggle(_ status: CollectionStatus, for id: String) {
        save(collection.toggling(status, for: id))
    }

    /// Replaces the collection with a backup file. Returns false when the
    /// file has no amiibo in it.
    @discardableResult
    func importBackup(_ data: Data) -> Bool {
        let imported = AmiiboCollection.parse(data)
        guard !imported.isEmpty else { return false }
        save(imported)
        return true
    }

    /// A backup in the same format the web app uses, so it moves between them.
    var backup: CollectionBackup {
        CollectionBackup(data: collection.encoded())
    }

    private func save(_ newCollection: AmiiboCollection) {
        collection = newCollection
        defaults.set(newCollection.encoded(), forKey: storageKey)
    }
}
