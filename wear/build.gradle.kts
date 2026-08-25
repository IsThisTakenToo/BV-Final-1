plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.spotvault.wear"
    // Matches the phone module's own compileSdk exactly (see app/build.gradle.kts) rather than
    // plain "36" — a mismatched revision here triggered Gradle to try auto-installing a second,
    // slightly different SDK Platform component, which stalled indefinitely fighting Android
    // Studio's own lock on the SDK directory instead of reusing what :app already has installed.
    compileSdk { version = release(36) { minorApiLevel = 1 } }

    defaultConfig {
        // Matches the phone app's applicationId — Google's convention for pairing a Wear OS app
        // with its phone companion in a single Play Store listing. The Data Layer APIs themselves
        // (DataClient/MessageClient) route by node pairing, not package identity, so this isn't a
        // functional requirement for the feature to work — only for eventual Play distribution.
        applicationId = "com.droppinvault.app.abceef"
        // Independent of the phone app's minSdk 26 — separate module, separate manifest. Wear OS's
        // own platform floor is API 25, but essentially all Play-relevant Wear hardware today is
        // Wear OS 3 (API 30)+, which is what Google's current guidance recommends targeting.
        minSdk = 30
        targetSdk = 36
        // Distinct from the phone app's versionCode (27) — Play Console requires unique version
        // codes across every APK sharing one listing at upload time.
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("debugConfig") {
            storeFile = file("${rootDir}/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug { signingConfig = signingConfigs.getByName("debugConfig") }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // CompassActivity is a native Compose-for-Wear screen — the Tile itself (ProtoLayout) can't
    // render a smoothly-rotating live arrow, so that one screen needs a real Activity instead.
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.material)
    // ListenableFuture's interface alone is on the classpath transitively via the tiles/
    // protolayout artifacts, but Futures.immediateFuture(...) is a util class that lives in full
    // Guava, not the lightweight listenablefuture-only stub — TileService.onTileRequest() has to
    // return a ListenableFuture, and this is the standard way to hand back an already-known value
    // synchronously without needing a coroutine-to-future bridge for what's fast, non-suspending
    // work (reading a local SharedPreferences boolean, building a layout tree).
    implementation("com.google.guava:guava:33.4.0-android")
    // RemoteActivityHelper only — this belongs on the watch side, since it launches something on
    // the *other* (phone) device, not the reverse.
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // CompassActivity only — Wear Compose Material (not the phone's compose-material3, a
    // different, round-screen-oriented component set) plus the platform's own live location for
    // the watch's own current position while walking, independent of the phone.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.wear.compose.material)
    implementation(libs.play.services.location)
}
