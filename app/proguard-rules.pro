# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.phonetester.app.model.** { *; }
-keep class com.phonetester.app.navigation.** { *; }
-keepclassmembers class com.phonetester.app.** {
    public <init>(...);
}

# Compose
-dontwarn androidx.compose.**

# CameraX
-keep class androidx.camera.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep serialization
-keepattributes Signature
-keepattributes *Annotation*
-keep class org.json.** { *; }