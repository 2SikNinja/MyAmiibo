import AmiiboCore
import SwiftUI

struct StatusButtons: View {
    enum Size {
        case compact
        case large
    }

    let amiibo: Amiibo
    var size: Size = .compact

    @Environment(CollectionStore.self) private var collectionStore

    var body: some View {
        let status = collectionStore.status(of: amiibo.id)

        HStack(spacing: 8) {
            statusButton(
                isActive: status == .owned,
                activeTitle: "Owned",
                inactiveTitle: "Own it",
                activeIcon: "checkmark.circle.fill",
                inactiveIcon: "checkmark.circle",
                tint: .green,
                accessibilityLabel: status == .owned
                    ? "Remove \(amiibo.name) from owned"
                    : "Mark \(amiibo.name) as owned"
            ) {
                collectionStore.toggle(.owned, for: amiibo.id)
            }

            statusButton(
                isActive: status == .wishlist,
                activeTitle: "Wishlist",
                inactiveTitle: "Want it",
                activeIcon: "star.fill",
                inactiveIcon: "star",
                tint: .orange,
                accessibilityLabel: status == .wishlist
                    ? "Remove \(amiibo.name) from wishlist"
                    : "Add \(amiibo.name) to wishlist"
            ) {
                collectionStore.toggle(.wishlist, for: amiibo.id)
            }
        }
        .sensoryFeedback(.selection, trigger: status)
    }

    @ViewBuilder
    private func statusButton(
        isActive: Bool,
        activeTitle: String,
        inactiveTitle: String,
        activeIcon: String,
        inactiveIcon: String,
        tint: Color,
        accessibilityLabel: String,
        action: @escaping () -> Void
    ) -> some View {
        let label = Label(
            isActive ? activeTitle : inactiveTitle,
            systemImage: isActive ? activeIcon : inactiveIcon
        )
        .frame(maxWidth: .infinity)
        .font(size == .compact ? .caption.weight(.medium) : .body.weight(.medium))
        .lineLimit(1)
        .minimumScaleFactor(0.8)

        Group {
            if isActive {
                Button(action: action) { label }
                    .buttonStyle(.borderedProminent)
            } else {
                Button(action: action) { label }
                    .buttonStyle(.bordered)
            }
        }
        .tint(tint)
        .controlSize(size == .compact ? .small : .large)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityAddTraits(isActive ? .isSelected : [])
    }
}
