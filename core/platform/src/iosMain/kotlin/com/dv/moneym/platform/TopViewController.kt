package com.dv.moneym.platform

import platform.UIKit.UIApplication
import platform.UIKit.UISceneActivationStateForegroundActive
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.UIKit.UIWindowScene

internal fun topViewController(): UIViewController? {
    val scenes = UIApplication.sharedApplication.connectedScenes
        .mapNotNull { it as? UIWindowScene }
    val scene = scenes.firstOrNull {
        it.activationState == UISceneActivationStateForegroundActive
    } ?: scenes.firstOrNull()

    val sceneWindows = scene?.windows?.mapNotNull { it as? UIWindow }.orEmpty()
    val window = sceneWindows.firstOrNull { it.keyWindow }
        ?: sceneWindows.firstOrNull()

    var vc: UIViewController? = (window ?: UIApplication.sharedApplication.keyWindow)?.rootViewController
    while (vc?.presentedViewController != null) {
        vc = vc.presentedViewController
    }
    return vc
}
