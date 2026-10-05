// swift-tools-version: 6.3
import PackageDescription
let package = Package(
    name: "BluePosGoSDK",
    platforms: [.iOS(.v13)],
    products: [.library(name: "BluePosGoSDK", targets: ["BluePosGoSDK"])],
    targets: [.binaryTarget(name: "BluePosGoSDK", path: "BluePosGoSDK.xcframework")]
)
