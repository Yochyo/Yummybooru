package de.yochyo.yummybooru.utils.general

import android.content.Context
import de.yochyo.yummybooru.api.entities.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class Cache(context: Context) {
    private val directory = context.cacheDir

    var cachedFiles = HashMap<String, Boolean>()
    private val notYetCached = ConcurrentHashMap<String, Resource>()

    companion object {
        private var _instance: Cache? = null
        private val cacheLock = Any()
        fun getCache(context: Context): Cache {
            synchronized(cacheLock) {
                _instance = Cache(context)
            }
            return _instance!!
        }
    }

    init {
        directory.mkdirs()
    }

    suspend fun cacheFile(id: String, res: Resource) {
        if (cachedFiles[id] == null) {
            withContext(Dispatchers.IO) {
                try {
                    val f = File(directory, id)
                    if (!f.exists()) {
                        notYetCached[id] = res
                        cachedFiles[id] = true
                        f.createNewFile()
                        res.loadIntoImageView(f)
                        notYetCached.remove(id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    e.log()
                }
            }
        }
    }

    suspend fun getCachedFile(id: String): Resource? {
        return withContext(Dispatchers.IO) {
            if (cachedFiles[id] != null) {
                val f = File(directory, id)
                return@withContext if (f.exists() && f.length() != 0L) Resource.fromFile(f)
                else notYetCached[id]
            } else null
        }
    }

    private fun removeCachedBitmap(id: String) {
        cachedFiles.remove(id)
        try {
            File(directory, id).delete()
            notYetCached.remove(id)
        } catch (e: Exception) {
            e.printStackTrace()
            e.log()
        }
    }

    suspend fun clearCache() {
        cachedFiles.clear()
        notYetCached.clear()
        withContext(Dispatchers.IO) {
            directory.listFiles().forEach {
                if (it.isFile) {
                    try {
                        it.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        e.log()
                    }
                }
            }
        }
    }

}

val Context.cache: Cache get() = Cache.getCache(this)