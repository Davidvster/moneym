# Google Drive backup — setup

The encrypted cloud backup uploads your data to a hidden, app-private folder in your
own Google Drive (`drive.appdata` scope — invisible in the normal Drive UI, only this
app can read it).

Auth uses native SDKs, **not** a browser/redirect flow:

- **Android** — Credential Manager (account picker → email) + Google Identity
  Authorization API (Drive access token).
- **iOS** — GoogleSignIn-iOS SDK via a small Swift→Kotlin bridge.

You do **not** need `google-services.json` — that file is for Firebase. This feature
only needs OAuth client IDs from the Google Cloud Console.

**The app works with no setup.** If the IDs below are absent, `GoogleAuthManager.isConfigured`
is `false`, the Drive backup section is hidden in Settings → Backup, and everything else
works normally. Local file backup/restore is unaffected.

---

## 1. Google Cloud Console — one-time

1. Create (or pick) a project at <https://console.cloud.google.com/>.
2. **APIs & Services → Library →** enable **Google Drive API**.
3. **APIs & Services → OAuth consent screen:**
   - User type: External (or Internal for a Workspace org).
   - Add scope `https://www.googleapis.com/auth/drive.appdata`.
   - Add your Google account under **Test users** while the app is unverified.

## 2. Create OAuth clients (APIs & Services → Credentials → Create credentials → OAuth client ID)

You need up to three clients:

| Client type | Needed for | Notes |
|-------------|-----------|-------|
| **Android** | Android Authorization API (on-device Drive token) | Package `com.dv.moneym` + your signing SHA-1 (below). |
| **Web**     | Android Credential Manager `serverClientId` | No redirect URI needed. This is the value Android uses, **not** the Android client ID. |
| **iOS**     | iOS GoogleSignIn | Bundle ID = the iOS app bundle id. |

Debug signing SHA-1 for this project:

```
C3:00:54:45:62:A9:89:BD:50:D7:81:4B:71:9E:54:2B:73:CB:3A:2D
```

Get it yourself with:

```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
  -storepass android -keypass android | grep SHA1
```

(Add the **release** SHA-1 too when you ship a release build.)

## 3. Wire the IDs into the app

### Android — `local.properties` (git-ignored)

Use the **Web** client ID as the server client ID:

```properties
googleOAuthServerClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

(or set env var `GOOGLE_OAUTH_SERVER_CLIENT_ID`). This feeds
`BuildConfig.GOOGLE_OAUTH_SERVER_CLIENT_ID`. The Android client (SHA-1 + package) is
matched implicitly on-device by the Authorization API — no string to paste.

### iOS — `iosApp/iosApp/Info.plist`

Set the **iOS** client ID and its reversed form as a URL scheme:

```xml
<key>GIDClientID</key>
<string>YOUR_IOS_CLIENT_ID.apps.googleusercontent.com</string>

<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLName</key>
    <string>com.dv.moneym.oauth</string>
    <key>CFBundleURLSchemes</key>
    <array>
      <string>com.googleusercontent.apps.YOUR_IOS_CLIENT_ID</string>
    </array>
  </dict>
</array>
```

The reversed scheme is the iOS client ID with the `...apps.googleusercontent.com`
order flipped to `com.googleusercontent.apps...` (drop the `.apps.googleusercontent.com`
suffix; keep the numeric/dashed id). GoogleSignIn reads `GIDClientID` automatically.

## 4. iOS — add the GoogleSignIn SDK (manual Xcode steps)

Kotlin/Native cannot call the Obj-C/Swift GoogleSignIn SDK directly, so it is added on
the Swift side and bridged. This repo already contains the Swift bridge
(`iosApp/iosApp/GoogleSignInBridge.swift`) and the launch wiring in `iOSApp.swift`.
You must:

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. **File → Add Package Dependencies…** → `https://github.com/google/GoogleSignIn-iOS`
   → add the **GoogleSignIn** product to the `iosApp` target.
3. Make sure `GoogleSignInBridge.swift` is a member of the `iosApp` target
   (it should be auto-added; verify in the File Inspector).
4. Confirm `Info.plist` has `GIDClientID` + the reversed-client-id URL scheme (step 3).

How the bridge works (no code changes needed):

- Kotlin declares `GoogleSignInBridge` (interface) + `GoogleSignInBridgeHolder` (object),
  exported through the `ComposeApp` framework.
- `iOSApp.swift` sets `GoogleSignInBridgeHolder.shared.instance = GoogleSignInBridgeImpl()`
  at launch and forwards `onOpenURL` to `GIDSignIn.sharedInstance.handle(_:)`.
- `IosGoogleAuthManager` (Kotlin) calls the bridge for sign-in / token / sign-out.

## 5. Verify

- **Android:** `./gradlew :composeApp:assembleDebug`, install, Settings → Backup →
  Connect → Credential Manager sheet → pick account → grant Drive → "Back up now".
  With `googleOAuthServerClientId` removed, the remote section is hidden.
- **iOS:** build & run, Settings → Backup → Connect → native GoogleSignIn sheet → same flow.
