package de.yochyo.yummybooru.utils.general

import android.content.Context
import de.yochyo.yummybooru.api.entities.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class Cache(context: Context) {
    companion object {
        private var _instance: Cache? = null
        fun getInstance(context: Context): Cache {
            if (_instance == null) _instance = object : Cache(context) {}
            return _instance!!
        }
    }

    private val directory = context.cacheDir
    private val notYetCached = ConcurrentHashMap<String, Resource>()

    init {
        directory.mkdirs()
    }

    suspend fun cacheFile(id: String, res: Resource) {
        withContext(Dispatchers.IO) {
            try {
                val f = File(directory, id)
                if (!f.exists()) {
                    notYetCached[id] = res
                    f.createNewFile()
                    res.loadInto(f)
                    notYetCached.remove(id)
                }
            } catch (e: Exception) {
                Logger.log(e, id)
                e.printStackTrace()
            }
        }
    }

    suspend fun getCachedFile(id: String): Resource? {
        return withContext(Dispatchers.IO) {
            val f = File(directory, id)
            return@withContext if (f.exists() && f.length() != 0L) Resource.fromFile(f)
            else notYetCached[id]
        }
    }

    private fun removeCachedBitmap(id: String) {
        try {
            File(directory, id).delete()
            notYetCached.remove(id)
        } catch (e: Exception) {
            Logger.log(e)
            e.printStackTrace()
        }
    }

    suspend fun clearCache() {
        notYetCached.clear()
        withContext(Dispatchers.IO) {
            directory.listFiles().forEach {
                if (it.isFile) {
                    try {
                        it.delete()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Logger.log(e)
                    }
                }
            }
        }
    }

}

inline val Context.cache: Cache get() = Cache.getInstance(this)