# MoneyM release proguard rules

# ── Koin ─────────────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}
-dontwarn org.koin.**

# ── SQLDelight ────────────────────────────────────────────────────────────────
-keep class app.cash.sqldelight.** { *; }
-keep class com.dv.moneym.data.**.db.** { *; }
-dontwarn app.cash.sqldelight.**

# ── kotlinx.serialization ────────────────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.dv.moneym.**$$serializer { *; }
-keepclassmembers class com.dv.moneym.** {
    *** Companion;
}
-keepclasseswithmembers class com.dv.moneym.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep @kotlinx.serialization.Serializable class * { *; }

# ── kotlinx.coroutines ───────────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Kermit logging ───────────────────────────────────────────────────────────
-keep class co.touchlab.kermit.** { *; }
-dontwarn co.touchlab.kermit.**

# ── multiplatform-settings ───────────────────────────────────────────────────
-keep class com.russhwolf.settings.** { *; }
-dontwarn com.russhwolf.settings.**

# ── AndroidX Compose ─────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Biometric ────────────────────────────────────────────────────────────────
-keep class androidx.biometric.** { *; }

# ── App entry points ─────────────────────────────────────────────────────────
-keep class com.dv.moneym.MainActivity { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ── Enum classes ─────────────────────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
