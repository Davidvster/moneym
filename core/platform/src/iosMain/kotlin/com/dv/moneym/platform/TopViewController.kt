package com.dv.moneym.platform

import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

internal fun topViewController(): UIViewController? {
    var vc: UIViewController? = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (vc?.presentedViewController != null) {
        vc = vc!!.presentedViewController
    }
    return vc
}
