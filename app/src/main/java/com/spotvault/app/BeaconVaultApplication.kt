package com.spotvault.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class BeaconVaultApplication : Application(), ImageLoaderFactory {
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
