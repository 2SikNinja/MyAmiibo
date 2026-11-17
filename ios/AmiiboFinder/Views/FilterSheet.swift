import AmiiboCore
import SwiftUI

struct FilterSheet: View {
    @Binding var filters: AmiiboFilters
    let catalog: [Amiibo]

    @Environment(\.dismiss) private var dismiss

    /// Series pickers only list values that exist for the chosen type.
    private var typeScoped: [Amiibo] {
        guard let type = filters.type else { return catalog }
        return catalog.filter { $0.type == type }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Type", selection: typeBinding) {
                        Text("All types").tag(String?.none)
                        ForEach(AmiiboSearch.uniqueValues(catalog, \.type), id: \.self) { type in
                            Text(type).tag(String?.some(type))
                        }
                    }

                    Picker("Amiibo series", selection: $filters.amiiboSeries) {
                        Text("All series").tag(String?.none)
                        ForEach(AmiiboSearch.uniqueValues(typeScoped, \.amiiboSeries), id: \.self) { series in
                            Text(series).tag(String?.some(series))
                        }
                    }
                    .pickerStyle(.navigationLink)

                    Picker("Game series", selection: $filters.gameSeries) {
                        Text("All game series").tag(String?.none)
                        ForEach(AmiiboSearch.uniqueValues(typeScoped, \.gameSeries), id: \.self) { series in
                            Text(series).tag(String?.some(series))
                        }
                    }
                    .pickerStyle(.navigationLink)
                }

                Section("Collection") {
                    Picker("Show", selection: $filters.ownership) {
                        ForEach(OwnershipFilter.allCases) { option in
                            Text(option.label).tag(option)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Section("Sort") {
                    Picker("Sort by", selection: $filters.sort) {
                        ForEach(SortOrder.allCases) { order in
                            Text(order.label).tag(order)
                        }
                    }
                    .pickerStyle(.inline)
                    .labelsHidden()
                }
            }
            .navigationTitle("Filters")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Reset") {
                        filters = AmiiboFilters(query: filters.query, sort: filters.sort)
                    }
                    .disabled(!filters.hasActiveFilters)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
        }
    }

    /// Changing the type clears series choices that may no longer exist.
    private var typeBinding: Binding<String?> {
        Binding(
            get: { filters.type },
            set: { newType in
                guard newType != filters.type else { return }
                filters.type = newType
                filters.amiiboSeries = nil
                filters.gameSeries = nil
            }
        )
    }
}
