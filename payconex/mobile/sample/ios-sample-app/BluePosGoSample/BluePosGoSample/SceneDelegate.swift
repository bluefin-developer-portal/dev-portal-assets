import Combine
import BluePosGoSDK
import SwiftUI
import UIKit

/// Scene-owned integration lifetime for iOS 13+. Keep the model alive while BluePOS Go is
/// foregrounded so its pending SDK completions still have a checkout to update on return.
final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    private let model = CheckoutModel()
    private let appearance = SampleAppearance()
    // Retain the Combine subscription for the scene lifetime; discarding it would stop updates.
    private var appearanceSubscription: AnyCancellable?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }
        let window = UIWindow(windowScene: windowScene)
        window.overrideUserInterfaceStyle = appearance.isDarkMode ? .dark : .light
        window.rootViewController = UIHostingController(
            rootView: CheckoutView().environmentObject(model).environmentObject(appearance)
        )
        // Override at the window so SwiftUI, UIKit controls, the logo and status bar all agree.
        // This also supports the sample's iOS 13 deployment target.
        appearanceSubscription = appearance.$isDarkMode.removeDuplicates().sink { [weak window] isDark in
            window?.overrideUserInterfaceStyle = isDark ? .dark : .light
        }
        self.window = window
        window.makeKeyAndVisible()

        // Cold launch may deliver the URL here instead of openURLContexts. Forward it after
        // constructing the model/window, but do not assume this restores a previous process's
        // in-memory SDK request/completion. Production recovery needs a persisted operation and
        // reconciliation. Never log the full URL; its query may contain sensitive response data.
        for context in connectionOptions.urlContexts {
            _ = BluePosGo.shared.handleCallback(url: context.url)
        }
    }

    // Warm returns arrive here. Forward to the SDK exactly as received; it owns decoding and
    // request correlation. This sample has no other deep-link handlers and ignores the Boolean;
    // a host app should route unhandled URLs to its other handlers without interpreting them as
    // successful payments. Keep AppDelegate's fallback entry point consistent with this one.
    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        for context in URLContexts {
            _ = BluePosGo.shared.handleCallback(url: context.url)
        }
    }
}
