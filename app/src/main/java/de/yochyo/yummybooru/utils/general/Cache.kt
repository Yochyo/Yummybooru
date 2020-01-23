package de.yochyo.yummybooru.utils.general

import android.content.Context
import de.yochyo.yummybooru.api.entities.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class Cache(path: String) {
    private val directory = File(path)

    var cachedFiles = HashMap<String, Boolean>()
    private val notYetCached = ConcurrentHashMap<String, Resource>()

    companion object {
        private var _instance: Cache? = null
        fun getInstance(path: String): Cache {
            if (_instance == null) _instance = object : Cache(path) {}
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
                        res.loadInto(f)
                        notYetCached.remove(id)
                    }
                } catch (e: Exception) {
                    Logger.log(e, id)
                    e.printStackTrace()
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
            Logger.log(e)
            e.printStackTrace()
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
                        Logger.log(e)
                    }
                }
            }
        }
    }

}

val Context.cache: Cache get() = Cache.getInstance(cacheDir.absolutePath)