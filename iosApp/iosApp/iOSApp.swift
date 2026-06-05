import SwiftUI
import GoogleSignIn
import Shared

@main
struct iOSApp: App {
    init() {
        GoogleSignInBridgeHolder.shared.instance = GoogleSignInBridgeImpl()
        IosAiBridgeHolder.shared.instance = FoundationModelsBridgeImpl()
        IosLocalLlmBridgeHolder.shared.instance = LiteRtLmBridgeImpl()
        GIDSignIn.sharedInstance.restorePreviousSignIn()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
