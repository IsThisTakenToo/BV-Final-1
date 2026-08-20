# DropPin Vault

Native **Android** app (Kotlin + Jetpack Compose) for saving, organizing, and navigating back to important locations.

> **Note:** This is **not** a web or React app. Open and run it with **Android Studio** only.

## Requirements

- [Android Studio](https://developer.android.com/studio) (latest stable)
- Android SDK 36
- JDK 11+

## Run locally

1. Open Android Studio → **File → Open** → select this folder (`BeaconVault`).
2. Wait for Gradle sync to finish (Android Studio will create the Gradle wrapper if needed).
3. Connect a device or start an emulator (API 24+).
4. Click **Run** (green play button) or use **Run → Run 'app'**.

Debug builds use the standard Android debug keystore automatically — no extra setup required.

## Build from the command line

After Android Studio has synced once (generates `gradlew`):

```bash
./gradlew :app:assembleDebug
```

On Windows:

```powershell
.\gradlew.bat :app:assembleDebug
```

## Project structure

| Path | Purpose |
|------|---------|
| `app/src/main/java/com/spotvault/app/` | App source (Compose UI, services, widgets) |
| `app/src/main/res/` | Icons, layouts, widgets, themes |
| `app/build.gradle.kts` | App module build config |
| `gradle/libs.versions.toml` | Dependency versions |

## Release builds

Release signing expects these environment variables (or a local keystore at the project root):

- `KEYSTORE_PATH`
- `STORE_PASSWORD`
- `KEY_PASSWORD`

## AI Studio

If you imported this from Google AI Studio, use **Android Studio** to preview and run the app on a device or emulator. Do not run `npm install` or `npm start` — this project has no Node/React toolchain.
