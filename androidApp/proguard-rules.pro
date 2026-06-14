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

# ── LiteRT-LM (local LLM, e.g. Gemma) ────────────────────────────────────────
# liblitertlm_jni.so resolves the JNI callback methods (onMessage/onDone) by
# literal name at runtime. R8 renaming them makes the native lookup throw
# NoSuchMethodError → SIGABRT on the first streamed reply. Keep names intact.
-keep class com.google.ai.edge.litertlm.** { *; }
-keepclassmembers class com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**

# ── ML Kit GenAI (Gemini Nano) ───────────────────────────────────────────────
-keep class com.google.mlkit.genai.** { *; }
-dontwarn com.google.mlkit.genai.**

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
