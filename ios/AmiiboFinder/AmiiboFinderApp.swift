import SwiftUI

@main
struct AmiiboFinderApp: App {
    @State private var catalogStore = CatalogStore()
    @State private var collectionStore = CollectionStore()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(catalogStore)
                .environment(collectionStore)
                .task {
                    await catalogStore.loadIfNeeded()
                }
        }
    }
}

struct RootView: View {
    var body: some View {
        TabView {
            NavigationStack {
                BrowseView()
            }
            .tabItem {
                Label("Browse", systemImage: "magnifyingglass")
            }

            NavigationStack {
                CollectionScreen()
            }
            .tabItem {
                Label("Collection", systemImage: "square.grid.2x2.fill")
            }
        }
    }
}
