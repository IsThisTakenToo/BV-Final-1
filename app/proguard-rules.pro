# Preserve line numbers for readable release stack traces.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin / coroutines
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn kotlinx.coroutines.**

# --- Room (annotation-based DB) ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * {
    <fields>;
}
-keep @androidx.room.Dao interface * {
    *;
}
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Entity fields used by Room and referenced in VaultBackupManager JSON keys.
-keep class com.spotvault.app.LocationSpot {
    <fields>;
}
-keep interface com.spotvault.app.LocationDao { *; }
-keep class com.spotvault.app.AppDatabase { *; }

# Backup import/export uses org.json with explicit string keys; keep manager + spot
# metadata stable across releases so exported spot.json remains compatible.
-keep class com.spotvault.app.VaultBackupManager { *; }
-keepclassmembers class com.spotvault.app.VaultBackupManager$SpotExportSummary {
    <fields>;
}

# Manifest components (widgets, services, receivers, workers)
-keep class com.spotvault.app.MainActivity { *; }
-keep class com.spotvault.app.TimerService { *; }
-keep class com.spotvault.app.*Receiver { *; }
-keep class com.spotvault.app.*Widget { *; }
-keep class com.spotvault.app.PremiumGlanceWidget { *; }
-keep class com.spotvault.app.PremiumGlanceWidgetReceiver { *; }
-keep class com.spotvault.app.SmallOutdoorWidget { *; }
-keep class com.spotvault.app.SmallOutdoorWidgetReceiver { *; }
-keep class com.spotvault.app.MediumOutdoorWidget { *; }
-keep class com.spotvault.app.MediumOutdoorWidgetReceiver { *; }
-keep class com.spotvault.app.BadassCommandCenterWidget { *; }
-keep class com.spotvault.app.BadassCommandCenterWidgetReceiver { *; }
-keep class com.spotvault.app.WatchdogWorker { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-keep class androidx.work.** { *; }

# ML Kit text recognition
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# Firebase App Check
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Google Play services / location
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# Credential Manager (Google Drive sign-in) — CredentialManager.getCredential() resolves its
# actual provider implementation (PlayServicesCredentialProviderFactory) via a manifest-declared
# class name at runtime, not a direct code reference, so R8 can't see that it's reachable and will
# strip or rename it under minification unless kept explicitly. The androidx.credentials AAR is
# supposed to ship this as a bundled consumer rule already, but it's cheap insurance to declare it
# here too rather than find out from a release-only crash report that it didn't take effect.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }
-dontwarn com.google.android.libraries.identity.googleid.**

# OSMDroid (OpenStreetMap tiles + MapView)
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# OSMBonusPack (marker clustering, utilities)
-keep class org.osmdroid.bonuspack.** { *; }
-dontwarn org.osmdroid.bonuspack.**

# BiometricPrompt
-keep class androidx.biometric.** { *; }

# Moshi (present on classpath; keep codegen adapters if enabled later)
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn com.squareup.moshi.**

# Compose runtime (library ships consumer rules; reinforce UI entry points)
-keep class com.spotvault.app.** {
    public <init>(...);
}

# Firebase Crashlytics
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
