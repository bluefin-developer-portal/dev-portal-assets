import SwiftUI

/// Colors observed on bluefin.com, with accessible native dark-mode equivalents.
/// Asset sources and design decisions are documented in mobile/sample/BRANDING.md.
enum BluefinTheme {
    static let navy = color(0x071D49)
    static let sky = color(0x00A9E0)
    static let yellow = color(0xFFCD00)
    static let action = adaptive(light: 0x0047BB, dark: 0x8ED8F8)
    static let ink = adaptive(light: 0x071D49, dark: 0xEEF5FF)
    static let muted = adaptive(light: 0x52627A, dark: 0xB8C9DD)
    static let background = adaptive(light: 0xF3F8FC, dark: 0x06142B)
    static let surface = adaptive(light: 0xFFFFFF, dark: 0x102544)
    static let wash = adaptive(light: 0xE8F4F9, dark: 0x173454)
    static let border = adaptive(light: 0xB8C9DB, dark: 0x506584)

    private static func uiColor(_ hex: UInt32) -> UIColor {
        UIColor(red: CGFloat((hex >> 16) & 255) / 255,
                green: CGFloat((hex >> 8) & 255) / 255,
                blue: CGFloat(hex & 255) / 255, alpha: 1)
    }
    private static func color(_ hex: UInt32) -> Color { Color(uiColor(hex)) }
    // Dynamic UIColor reads the window's overridden trait collection, so SwiftUI colors follow
    // the in-app preference alongside UIKit controls and the light/dark logo asset.
    private static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(UIColor { traits in uiColor(traits.userInterfaceStyle == .dark ? dark : light) })
    }
}

/// The diagonal motif echoes the Bluefin mark and website hero without changing the logo.
struct BluefinAngle: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        path.move(to: CGPoint(x: rect.width * 0.64, y: 0))
        path.addLine(to: CGPoint(x: rect.width, y: 0))
        path.addLine(to: CGPoint(x: rect.width, y: rect.height))
        path.addLine(to: CGPoint(x: rect.width * 0.22, y: rect.height))
        path.closeSubpath()
        return path
    }
}

extension View {
    // Retain the iOS 13 fallback: the sample supports devices older than SwitchToggleStyle(tint:).
    @ViewBuilder func bluefinToggleTint() -> some View {
        if #available(iOS 14, *) {
            self.toggleStyle(SwitchToggleStyle(tint: BluefinTheme.action))
        } else {
            self.accentColor(BluefinTheme.action)
        }
    }
}

/// A local display preference, deliberately independent of payment configuration.
@MainActor
final class SampleAppearance: ObservableObject {
    private let defaults: UserDefaults
    private static let darkModeKey = "sample.darkMode"

    @Published var isDarkMode: Bool {
        didSet { defaults.set(isDarkMode, forKey: Self.darkModeKey) }
    }

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        // Test key presence: bool(forKey:) alone returns false for a missing key, which would
        // erase the required default-dark behavior. A stored false is the user's light choice.
        self.isDarkMode = defaults.object(forKey: Self.darkModeKey) == nil
            ? true : defaults.bool(forKey: Self.darkModeKey)
    }
}
