# Add project specific ProGuard rules here.

# Keep Compose runtime metadata (handled by AGP defaults, kept here for clarity).
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
