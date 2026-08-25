package com.spotvault.app

import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Process
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class BeaconVaultApplication : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Runs in every process this Application class is instantiated in, including
        // CrashRecoveryActivity's own separate :crash process — harmless there (that activity is
        // deliberately built to not crash, and if it somehow did, this would just route it through
        // the exact same recovery flow again in its own process, never affecting the main app).
        installCrashHandler(this)

        // Everything below here is real app work (Wear OS sync) that only belongs in the main
        // process — CrashRecoveryActivity's :crash process deliberately touches nothing beyond a
        // single boolean prefs read (see its own file comment), and this must not add to that.
        if (!isMainProcess()) return
        // Catch-up push for the watch's own tracking-state cache, in case it was unpaired/off when
        // tracking last started/stopped on the phone. Also subsumes the boot-completed case, since
        // a process has to exist for BootReceiver to matter at all.
        //
        // Both calls are wrapped — this is best-effort background sync (already documented as
        // fire-and-forget at each call site), and Application.onCreate() throwing anything at all
        // takes the entire app down before a single frame ever draws, with no recovery path except
        // CrashRecoveryActivity. A first-ever cold start is exactly where Play Services' Wearable
        // client is least likely to have anything cached or ready yet, so if any part of standing
        // one up can throw synchronously rather than only failing inside its own async Task, this
        // is the worst possible place to let that propagate.
        try {
            TrackingWearSync.pushTrackingState(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Same catch-up reasoning for the watch tile's colors/button style, in case the watch
        // missed the change (unpaired/off at the time) or was paired for the first time since.
        try {
            ThemeWearSync.pushThemeState(this, getSharedPreferences("SpotVaultPrefs", MODE_PRIVATE))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isMainProcess(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return getProcessName() == packageName
        }
        val manager = getSystemService(ActivityManager::class.java) ?: return true
        val pid = Process.myPid()
        return manager.runningAppProcesses.orEmpty().any { it.pid == pid && it.processName == packageName }
    }

    /**
     * Caps Coil's disk cache so years of vault/widget thumbs cannot grow toward the default
     * (~2% of free space). Memory stays a modest heap fraction; decode sizes are still set per
     * request elsewhere.
     */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.18)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("coil_image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024)
                    .build()
            }
            .build()
}
