import AmiiboCore
import SwiftUI

struct BrowseView: View {
    @Environment(CatalogStore.self) private var catalogStore
    @Environment(CollectionStore.self) private var collectionStore

    @State private var filters = AmiiboFilters.defaults
    @State private var isShowingFilters = false

    private let columns = [GridItem(.adaptive(minimum: 160), spacing: 12)]

    private var results: [Amiibo] {
        AmiiboSearch.filter(catalogStore.catalog, filters: filters, collection: collectionStore.collection)
    }

    var body: some View {
        content
            .navigationTitle("MyAmiibo")
            .navigationDestination(for: Amiibo.self) { amiibo in
                AmiiboDetailView(amiibo: amiibo)
            }
            .searchable(text: $filters.query, prompt: "Name, character, or series")
            .autocorrectionDisabled()
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        isShowingFilters = true
                    } label: {
                        Label(
                            "Filters",
                            systemImage: filters.hasActiveFilters
                                ? "line.3.horizontal.decrease.circle.fill"
                                : "line.3.horizontal.decrease.circle"
                        )
                    }
                    .disabled(catalogStore.catalog.isEmpty)
                }
            }
            .sheet(isPresented: $isShowingFilters) {
                FilterSheet(filters: $filters, catalog: catalogStore.catalog)
                    .presentationDetents([.medium, .large])
            }
    }

    @ViewBuilder
    private var content: some View {
        switch catalogStore.state {
        case .idle, .loading:
            ProgressView("Loading amiibo…")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .failed(let message):
            ContentUnavailableView {
                Label("Couldn't load amiibo", systemImage: "wifi.exclamationmark")
            } description: {
                Text(message)
            } actions: {
                Button("Try Again") {
                    Task { await catalogStore.refresh() }
                }
                .buttonStyle(.borderedProminent)
            }
        case .loaded:
            let currentResults = results
            if currentResults.isEmpty {
                noResults
            } else {
                ScrollView {
                    Text("\(currentResults.count) results")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal)

                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(currentResults) { amiibo in
                            AmiiboCard(amiibo: amiibo)
                        }
                    }
                    .padding(.horizontal)
                    .padding(.bottom)
                }
                .background(Color(.systemGroupedBackground))
                .refreshable {
                    await catalogStore.refresh()
                }
            }
        }
    }

    @ViewBuilder
    private var noResults: some View {
        if !filters.query.isEmpty {
            ContentUnavailableView.search(text: filters.query)
        } else {
            ContentUnavailableView {
                Label("No matches", systemImage: "line.3.horizontal.decrease.circle")
            } description: {
                Text("No amiibo match these filters.")
            } actions: {
                Button("Reset Filters") {
                    filters = AmiiboFilters(sort: filters.sort)
                }
            }
        }
    }
}
