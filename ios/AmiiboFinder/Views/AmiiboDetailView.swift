import AmiiboCore
import SwiftUI

struct AmiiboDetailView: View {
    let amiibo: Amiibo

    @Environment(CatalogStore.self) private var catalogStore

    @State private var detail: AmiiboDetail?
    @State private var isLoadingUsage = true
    @State private var usageError: String?

    var body: some View {
        List {
            Section {
                AmiiboImage(url: amiibo.imageURL)
                    .frame(height: 260)
                    .frame(maxWidth: .infinity)
                    .listRowBackground(Color.clear)
                    .accessibilityLabel(amiibo.name)

                StatusButtons(amiibo: amiibo, size: .large)
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets())
            }

            Section("Details") {
                LabeledContent("Character", value: amiibo.character)
                LabeledContent("Amiibo series", value: amiibo.amiiboSeries)
                LabeledContent("Game series", value: amiibo.gameSeries)
                LabeledContent("Type", value: amiibo.type)
            }

            Section("Release dates") {
                ForEach(amiibo.release.byRegion) { release in
                    LabeledContent(release.region, value: Self.formatDate(release.date))
                }
            }

            usageSections
        }
        .listStyle(.insetGrouped)
        .navigationTitle(amiibo.name)
        .navigationBarTitleDisplayMode(.inline)
        .task(id: amiibo.id) {
            await loadUsage()
        }
    }

    @ViewBuilder
    private var usageSections: some View {
        if isLoadingUsage {
            Section("Game usage") {
                HStack {
                    ProgressView()
                    Text("Loading games…")
                        .foregroundStyle(.secondary)
                }
            }
        } else if let usageError {
            Section("Game usage") {
                VStack(alignment: .leading, spacing: 8) {
                    Text(usageError)
                        .foregroundStyle(.secondary)
                    Button("Try Again") {
                        Task { await loadUsage() }
                    }
                }
            }
        } else if let detail, !detail.usage.isEmpty {
            ForEach(detail.usage) { platform in
                Section {
                    ForEach(platform.games) { game in
                        GameUsageRow(game: game)
                    }
                } header: {
                    Text("\(platform.platform) · \(platform.games.count) \(platform.games.count == 1 ? "game" : "games")")
                }
            }
        } else {
            Section("Game usage") {
                Text("AmiiboAPI doesn't list any game features for this amiibo.")
                    .foregroundStyle(.secondary)
            }
        }
    }

    private func loadUsage() async {
        isLoadingUsage = true
        usageError = nil
        do {
            detail = try await catalogStore.detail(for: amiibo.id)
        } catch {
            // Leaving the screen cancels the request; that isn't a failure.
            if Task.isCancelled { return }
            usageError = "Couldn't load game usage. Check your connection."
        }
        isLoadingUsage = false
    }

    private static let inputFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(identifier: "UTC")
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

    private static func formatDate(_ date: String?) -> String {
        guard let date else { return "Not released" }
        guard let parsed = inputFormatter.date(from: date) else { return date }
        return parsed.formatted(Date.FormatStyle(date: .abbreviated, time: .omitted, timeZone: TimeZone(identifier: "UTC")!))
    }
}

private struct GameUsageRow: View {
    let game: GameUsage

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(game.gameName)
                .font(.body.weight(.medium))
            ForEach(Array(game.usages.enumerated()), id: \.offset) { _, usage in
                HStack(alignment: .firstTextBaseline, spacing: 6) {
                    Text(usage.description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                    if usage.writesData {
                        Text("SAVES DATA")
                            .font(.caption2.weight(.semibold))
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(Color.accentColor.opacity(0.15), in: Capsule())
                            .foregroundStyle(Color.accentColor)
                            .accessibilityLabel("Saves data to the amiibo")
                    }
                }
            }
        }
        .padding(.vertical, 2)
    }
}
