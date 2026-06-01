import SwiftUI
import GoogleSignIn
import ComposeApp

@main
struct iOSApp: App {
    init() {
        GoogleSignInBridgeHolder.shared.instance = GoogleSignInBridgeImpl()
        IosAiBridgeHolder.shared.instance = FoundationModelsBridgeImpl()
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
