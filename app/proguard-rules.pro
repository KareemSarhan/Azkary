# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# =============================================================================
# Kotlin Serialization
# =============================================================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# =============================================================================
# Retrofit & Networking
# =============================================================================
-dontwarn javax.annotation.**
-keepattributes Signature
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keepclasseswithmembers interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# =============================================================================
# Room Database
# =============================================================================
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# =============================================================================
# Hilt / Dagger
# =============================================================================
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* <fields>;
    @javax.inject.* <init>(...);
}
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.lang.model.element.Modifier

# =============================================================================
# DataStore
# =============================================================================
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# =============================================================================
# Compose Navigation
# =============================================================================
-keep class * {
    @androidx.navigation.compose.NavDestination <methods>;
}

# =============================================================================
# App Models (keep for serialization)
# =============================================================================
-keep class com.app.azkary.data.model.** { *; }
-keep class com.app.azkary.data.network.dto.** { *; }
-keep class com.app.azkary.data.local.entities.** { *; }
-keep class com.app.azkary.data.prefs.ThemeSettings { *; }

# =============================================================================
# Play Services Location
# =============================================================================
-dontwarn com.google.android.gms.internal.location.zze
-keep class com.google.android.gms.internal.location.** { *; }