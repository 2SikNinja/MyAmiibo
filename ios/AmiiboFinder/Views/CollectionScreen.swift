import AmiiboCore
import SwiftUI
import UniformTypeIdentifiers

struct CollectionScreen: View {
    @Environment(CatalogStore.self) private var catalogStore
    @Environment(CollectionStore.self) private var collectionStore

    @State private var selectedTab: CollectionStatus = .owned
    @State private var isImporting = false
    @State private var pendingImport: Data?
    @State private var alertMessage: String?

    private var stats: CollectionStats {
        CollectionStats(catalog: catalogStore.catalog, collection: collectionStore.collection)
    }

    private var tabItems: [Amiibo] {
        let ids = selectedTab == .owned ? collectionStore.collection.owned : collectionStore.collection.wishlist
        let byId = Dictionary(uniqueKeysWithValues: catalogStore.catalog.map { ($0.id, $0) })
        return AmiiboSearch.sort(ids.compactMap { byId[$0] }, by: .name)
    }

    var body: some View {
        let currentStats = stats

        List {
            Section {
                HStack(spacing: 12) {
                    StatTile(label: "Owned", value: currentStats.ownedCount, detail: "\(currentStats.percent)% of \(currentStats.totalCount)")
                    StatTile(label: "Wishlist", value: currentStats.wishlistCount, detail: "to find")
                    StatTile(label: "Series", value: currentStats.series.count, detail: "\(currentStats.completedSeriesCount) complete")
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }

            if !currentStats.series.isEmpty {
                Section("Series progress") {
                    ForEach(currentStats.series) { entry in
                        VStack(alignment: .leading, spacing: 6) {
                            HStack {
                                Text(entry.series)
                                    .lineLimit(1)
                                Spacer()
                                Text("\(entry.owned)/\(entry.total)")
                                    .monospacedDigit()
                                    .foregroundStyle(.secondary)
                            }
                            .font(.subheadline)
                            ProgressView(value: entry.fraction)
                                .tint(entry.owned == entry.total ? .green : .accentColor)
                        }
                        .accessibilityElement(children: .combine)
                    }
                }
            }

            Section {
                Picker("List", selection: $selectedTab) {
                    Text("Owned (\(currentStats.ownedCount))").tag(CollectionStatus.owned)
                    Text("Wishlist (\(currentStats.wishlistCount))").tag(CollectionStatus.wishlist)
                }
                .pickerStyle(.segmented)
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())

                if tabItems.isEmpty {
                    Text(selectedTab == .owned
                         ? "You haven't marked any amiibo as owned yet. Tap Own it on any amiibo in Browse."
                         : "Your wishlist is empty. Tap Want it on any amiibo in Browse.")
                        .foregroundStyle(.secondary)
                        .padding(.vertical, 8)
                } else {
                    ForEach(tabItems) { amiibo in
                        NavigationLink(value: amiibo) {
                            CollectionRow(amiibo: amiibo)
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                collectionStore.toggle(selectedTab, for: amiibo.id)
                            } label: {
                                Label("Remove", systemImage: "trash")
                            }
                        }
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle("My Collection")
        .navigationDestination(for: Amiibo.self) { amiibo in
            AmiiboDetailView(amiibo: amiibo)
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    ShareLink(
                        item: collectionStore.backup,
                        preview: SharePreview("Amiibo collection backup")
                    ) {
                        Label("Export Backup", systemImage: "square.and.arrow.up")
                    }
                    Button {
                        isImporting = true
                    } label: {
                        Label("Import Backup", systemImage: "square.and.arrow.down")
                    }
                } label: {
                    Label("Backup", systemImage: "ellipsis.circle")
                }
            }
        }
        .fileImporter(isPresented: $isImporting, allowedContentTypes: [.json]) { result in
            handleImport(result)
        }
        .confirmationDialog(
            "Replace your collection?",
            isPresented: Binding(get: { pendingImport != nil }, set: { if !$0 { pendingImport = nil } }),
            titleVisibility: .visible
        ) {
            Button("Replace", role: .destructive) {
                if let data = pendingImport {
                    applyImport(data)
                }
                pendingImport = nil
            }
        } message: {
            Text("The backup will replace the amiibo currently on this device.")
        }
        .alert(
            alertMessage ?? "",
            isPresented: Binding(get: { alertMessage != nil }, set: { if !$0 { alertMessage = nil } })
        ) {
            Button("OK", role: .cancel) {}
        }
    }

    private func handleImport(_ result: Result<URL, Error>) {
        guard case .success(let url) = result else { return }
        let hasAccess = url.startAccessingSecurityScopedResource()
        defer {
            if hasAccess { url.stopAccessingSecurityScopedResource() }
        }
        guard let data = try? Data(contentsOf: url) else {
            alertMessage = "Couldn't read that file."
            return
        }
        if AmiiboCollection.parse(data).isEmpty {
            alertMessage = "That file has no amiibo in it, so nothing changed."
        } else if collectionStore.collection.isEmpty {
            applyImport(data)
        } else {
            pendingImport = data
        }
    }

    private func applyImport(_ data: Data) {
        let imported = AmiiboCollection.parse(data)
        collectionStore.importBackup(data)
        alertMessage = "Imported \(imported.owned.count) owned and \(imported.wishlist.count) wishlist amiibo."
    }
}

private struct StatTile: View {
    let label: String
    let value: Int
    let detail: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value, format: .number)
                .font(.title.weight(.semibold))
                .monospacedDigit()
                .contentTransition(.numericText())
            Text(detail)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
        .accessibilityElement(children: .combine)
    }
}

private struct CollectionRow: View {
    let amiibo: Amiibo

    var body: some View {
        HStack(spacing: 12) {
            AmiiboImage(url: amiibo.imageURL)
                .frame(width: 48, height: 48)
            VStack(alignment: .leading, spacing: 2) {
                Text(amiibo.name)
                    .font(.body.weight(.medium))
                    .lineLimit(1)
                Text(amiibo.amiiboSeries)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
    }
}
