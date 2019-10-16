package de.yochyo.yummybooru.api.downloads

import android.content.Context
import de.yochyo.yummybooru.utils.Logger
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
    private val notYetCached = ConcurrentHashMap<String, ByteArray>()

    init {
        directory.mkdirs()
    }

    suspend fun cacheFile(id: String, image: ByteArray) {
        withContext(Dispatchers.IO) {
            try {
                val f = File(directory, id)
                if (!f.exists()) {
                    notYetCached[id] = image
                    f.createNewFile()
                    f.writeBytes(image)
                    notYetCached.remove(id)
                }
            } catch (e: Exception) {
                Logger.log(e, id)
                e.printStackTrace()
            }
        }
    }

    suspend fun getCachedFile(id: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            val f = File(directory, id)
            if (f.exists() && f.length() != 0L) {
                val stream = f.inputStream()
                val byteArray = stream.readBytes()
                stream.close()
                byteArray
            } else notYetCached[id]
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