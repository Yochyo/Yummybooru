package de.yochyo.yummybooru.utils.cache

import android.util.LruCache
import de.yochyo.yummybooru.api.entities.Resource


class MemoryCache : LruCache<String, Resource>((Runtime.getRuntime().maxMemory() / 1024).toInt() / 8) {
    override fun sizeOf(key: String, bitmap: Resource): Int {
        return bitmap.resource.size / 1024
    }
}