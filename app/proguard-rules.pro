# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# WebView JavaScript interface
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# Keep WebView related classes
-keep class android.webkit.** { *; }

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }

# Biometric
-keep class androidx.biometric.** { *; }
