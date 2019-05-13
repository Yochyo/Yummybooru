package de.yochyo.ybooru.api.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap

abstract class Cache(context: Context) {
    private val notYetCached = ConcurrentHashMap<String, Bitmap>()


    private val path = "${context.cacheDir}/"
    private val directory = File(path)

    init {
        directory.mkdirs()
    }

    companion object {
        private var _instance: Cache? = null
        fun getInstance(context: Context): Cache {
            if (_instance == null) _instance = object : Cache(context) {}
            return _instance!!
        }
    }

    suspend fun getCachedBitmap(id: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            val f = file(id)
            if (f.exists() && f.length() != 0L) {
                val stream = f.inputStream()
                val bitmap = BitmapFactory.decodeStream(stream)
                stream.close()
                bitmap
            } else notYetCached[id]
        }
    }

    suspend fun cacheBitmap(id: String, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            try {
                val f = file(id)
                if (!f.exists()) {
                    notYetCached[id] = bitmap
                    f.createNewFile()
                    val output = ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }
                    f.writeBytes(output.toByteArray())
                    output.close()
                    notYetCached.remove(id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun removeCachedBitmap(id: String) {
        try {
            file(id).delete()
            notYetCached.remove(id)
        } catch (e: Exception) {
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
                    }
                }
            }
        }
    }

    private fun file(id: String) = File("$path$id")
}

inline val Context.cache: Cache get() = Cache.getInstance(this)