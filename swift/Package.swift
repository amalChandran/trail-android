// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "Trail",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "TrailCore", targets: ["TrailCore"]),
        .library(name: "TrailEffects", targets: ["TrailEffects"]),
        .library(name: "TrailUI", targets: ["TrailUI"]),
        .library(name: "TrailMapKit", targets: ["TrailMapKit"]),
        .library(name: "TrailSamplePlugin", targets: ["TrailSamplePlugin"]),
    ],
    targets: [
        .target(name: "TrailCore"),
        .target(name: "TrailEffects", dependencies: ["TrailCore"]),
        .target(name: "TrailUI", dependencies: ["TrailCore"]),
        .target(name: "TrailMapKit", dependencies: ["TrailUI"]),
        .target(name: "TrailSamplePlugin", dependencies: ["TrailCore"]),
        .testTarget(name: "TrailCoreTests", dependencies: ["TrailCore", "TrailEffects", "TrailSamplePlugin"]),
    ]
)
