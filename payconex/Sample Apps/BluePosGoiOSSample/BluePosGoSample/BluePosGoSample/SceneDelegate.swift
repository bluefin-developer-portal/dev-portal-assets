import BluePosGoSDK
import SwiftUI
import UIKit

final class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?
    private let model = CheckoutModel()

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }
        let window = UIWindow(windowScene: windowScene)
        window.rootViewController = UIHostingController(
            rootView: CheckoutView().environmentObject(model)
        )
        self.window = window
        window.makeKeyAndVisible()

        for context in connectionOptions.urlContexts {
            _ = BluePosGo.shared.handleCallback(url: context.url)
        }
    }

    func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
        for context in URLContexts {
            _ = BluePosGo.shared.handleCallback(url: context.url)
        }
    }
}
