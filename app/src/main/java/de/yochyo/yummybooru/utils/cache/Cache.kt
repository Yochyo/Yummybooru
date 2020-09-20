package de.yochyo.yummybooru.utils.cache

import android.content.Context
import de.yochyo.yummybooru.api.entities.BitmapResource

class Cache(context: Context) {
    private val memoryCache = MemoryCache()
    private val diskCache = DiskCache(context.cacheDir)

    companion object {
        private val lock = Any()
        private var cache: Cache? = null
        fun getCache(context: Context): Cache {
            return synchronized(lock) {
                if (cache == null) cache = Cache(context)
                cache!!
            }
        }
    }

    suspend fun cache(key: String, res: BitmapResource) {
        memoryCache.put(key, res)
        diskCache.cacheResource(key, res)
        memoryCache.remove(key)
    }

    suspend fun getResource(key: String): BitmapResource? {
        var res = memoryCache.get(key)
        if (res == null)
            res = diskCache.getResource(key)
        return res
    }
}

val Context.cache get() = Cache.getCache(this)