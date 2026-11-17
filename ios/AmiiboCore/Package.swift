// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "AmiiboCore",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "AmiiboCore", targets: ["AmiiboCore"]),
    ],
    targets: [
        .target(name: "AmiiboCore"),
        .testTarget(name: "AmiiboCoreTests", dependencies: ["AmiiboCore"]),
    ]
)
