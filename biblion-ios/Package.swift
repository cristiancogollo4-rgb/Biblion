// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "Biblion",
    defaultLocalization: "es",
    platforms: [.iOS(.v17)],
    products: [.library(name: "Biblion", targets: ["Biblion"])],
    targets: [
        // Do not use host pkg-config paths when cross-compiling from Linux to iOS.
        .systemLibrary(name: "CSQLite", providers: [.apt(["libsqlite3-dev"])]),
        .target(name: "BiblionCore", dependencies: ["CSQLite"], resources: [.copy("Resources/bible_content.db"), .copy("Resources/dictionary_v2.db"), .copy("Resources/topics.db"), .copy("Resources/strongs.db"), .copy("Resources/cross_references_votes.db")]),
        .target(name: "Biblion", dependencies: ["BiblionCore"], resources: [.process("Resources")]),
        .testTarget(name: "BiblionCoreTests", dependencies: ["BiblionCore"])
    ],
    swiftLanguageModes: [.v6]
)
