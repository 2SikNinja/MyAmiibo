import CoreTransferable
import Foundation
import UniformTypeIdentifiers

/// Shared as a JSON file. The file is only written when the user actually shares.
struct CollectionBackup: Transferable {
    let data: Data

    static var transferRepresentation: some TransferRepresentation {
        FileRepresentation(exportedContentType: .json) { backup in
            let url = FileManager.default.temporaryDirectory
                .appendingPathComponent("amiibo-collection.json")
            try backup.data.write(to: url, options: .atomic)
            return SentTransferredFile(url)
        }
    }
}
