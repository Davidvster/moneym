import Foundation
import UIKit
import GoogleSignIn
import Shared

final class GoogleSignInBridgeImpl: NSObject, GoogleSignInBridge {

    func signIn(scopes: [String], onResult: @escaping (String?, String?) -> Void) {
        guard let presenter = Self.topViewController() else {
            onResult(nil, "No presenting view controller")
            return
        }
        GIDSignIn.sharedInstance.signIn(
            withPresenting: presenter,
            hint: nil,
            additionalScopes: scopes
        ) { result, error in
            if let error = error {
                onResult(nil, error.localizedDescription)
                return
            }
            onResult(result?.user.profile?.email, nil)
        }
    }

    func currentAccessToken(onResult: @escaping (String?, String?) -> Void) {
        guard let user = GIDSignIn.sharedInstance.currentUser else {
            onResult(nil, "Not signed in")
            return
        }
        user.refreshTokensIfNeeded { user, error in
            if let error = error {
                onResult(nil, error.localizedDescription)
                return
            }
            onResult(user?.accessToken.tokenString, nil)
        }
    }

    func restore(onResult: @escaping (String?) -> Void) {
        GIDSignIn.sharedInstance.restorePreviousSignIn { user, _ in
            onResult(user?.profile?.email)
        }
    }

    func signOut() {
        GIDSignIn.sharedInstance.signOut()
    }

    private static func topViewController() -> UIViewController? {
        let scenes = UIApplication.shared.connectedScenes
        let window = scenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow }
            .first
        var top = window?.rootViewController
        while let presented = top?.presentedViewController {
            top = presented
        }
        return top
    }
}
