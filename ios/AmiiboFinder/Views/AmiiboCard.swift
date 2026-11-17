import AmiiboCore
import SwiftUI

struct AmiiboCard: View {
    let amiibo: Amiibo

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // The link and the status buttons are siblings so tapping a
            // button never also opens the detail screen.
            NavigationLink(value: amiibo) {
                VStack(alignment: .leading, spacing: 6) {
                    AmiiboImage(url: amiibo.imageURL)
                        .padding(10)
                        .aspectRatio(1, contentMode: .fit)
                        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 10))

                    Text(amiibo.name)
                        .font(.subheadline.weight(.semibold))
                        .lineLimit(2, reservesSpace: true)
                        .multilineTextAlignment(.leading)

                    Text(amiibo.amiiboSeries)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            StatusButtons(amiibo: amiibo)
        }
        .padding(10)
        .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 14))
        .overlay {
            RoundedRectangle(cornerRadius: 14)
                .strokeBorder(Color(.separator).opacity(0.5))
        }
    }
}
