package com.dv.moneym.core.oauth

import io.ktor.http.Url
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AuthenticationServices.ASWebAuthenticationSession
import platform.AuthenticationServices.ASWebAuthenticationPresentationContextProvidingProtocol
import platform.AuthenticationServices.ASPresentationAnchor
import platform.Foundation.NSURL
import platform.Foundation.NSURLComponents
import platform.UIKit.UIApplication
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class IosAuthorizationLauncher : AuthorizationLauncher {

    private val presentationProvider = PresentationProvider()

    override suspend fun launch(request: AuthorizationRequest, redirectUri: String): AuthorizationResponse {
        return suspendCancellableCoroutine { cont ->
            val scheme = Url(redirectUri).protocol.name
            val session = ASWebAuthenticationSession(
                uRL = NSURL(string = request.url),
                callbackURLScheme = scheme,
            ) { callbackUrl, error ->
                if (error != null) {
                    cont.resumeWithException(GoogleAuthError.UserCancelled())
                    return@ASWebAuthenticationSession
                }
                if (callbackUrl == null) {
                    cont.resumeWithException(GoogleAuthError.Platform("Empty callback URL"))
                    return@ASWebAuthenticationSession
                }
                val parsed = parseCallback(callbackUrl)
                if (parsed == null) {
                    cont.resumeWithException(GoogleAuthError.Platform("Could not parse callback URL"))
                } else {
                    cont.resume(parsed)
                }
            }
            session.presentationContextProvider = presentationProvider
            session.prefersEphemeralWebBrowserSession = false
            val started = session.start()
            if (!started) {
                cont.resumeWithException(GoogleAuthError.Platform("ASWebAuthenticationSession.start() returned false"))
            }
            cont.invokeOnCancellation { session.cancel() }
        }
    }

    private fun parseCallback(callbackUrl: NSURL): AuthorizationResponse? {
        val components = NSURLComponents.componentsWithURL(callbackUrl, resolvingAgainstBaseURL = false) ?: return null
        val items = components.queryItems ?: return null
        var code: String? = null
        var state: String? = null
        var error: String? = null
        items.forEach { itemAny ->
            val item = itemAny as platform.Foundation.NSURLQueryItem
            when (item.name) {
                "code" -> code = item.value
                "state" -> state = item.value
                "error" -> error = item.value
            }
        }
        if (error != null) return null
        val c = code ?: return null
        val s = state ?: return null
        return AuthorizationResponse(c, s)
    }

    private class PresentationProvider : NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
        override fun presentationAnchorForWebAuthenticationSession(
            session: ASWebAuthenticationSession,
        ): ASPresentationAnchor {
            val windows = UIApplication.sharedApplication.windows
            return windows.firstOrNull { (it as platform.UIKit.UIWindow).isKeyWindow() }
                as ASPresentationAnchor? ?: windows.first() as ASPresentationAnchor
        }
    }
}
