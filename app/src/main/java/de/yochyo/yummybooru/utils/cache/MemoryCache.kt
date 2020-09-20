package de.yochyo.yummybooru.utils.cache

import android.util.LruCache
import de.yochyo.yummybooru.api.entities.BitmapResource


class MemoryCache : LruCache<String, BitmapResource>((Runtime.getRuntime().maxMemory() / 1024).toInt() / 8) {
    override fun sizeOf(key: String, bitmap: BitmapResource): Int {
        return bitmap.bitmap.allocationByteCount / 1024
    }
}