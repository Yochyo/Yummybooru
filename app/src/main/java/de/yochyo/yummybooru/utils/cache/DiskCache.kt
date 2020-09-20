package de.yochyo.yummybooru.utils.cache

import android.graphics.Bitmap
import com.jakewharton.disklrucache.DiskLruCache
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.api.entities.BitmapResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class DiskCache(private val dir: File) {
    private var _diskLruCache: DiskLruCache? = null
    private val lock = Mutex()

    private suspend fun getLruCache(): DiskLruCache {
        return withContext(Dispatchers.IO) {
            lock.withLock {
                if (_diskLruCache == null) _diskLruCache = DiskLruCache.open(dir, BuildConfig.VERSION_CODE, 1, 1024 * 1024 * 512)
                _diskLruCache!!
            }
        }
    }

    suspend fun cacheResource(key: String, res: BitmapResource) {
        withContext(Dispatchers.IO) {
            val editor = getLruCache().edit(key)
            if (editor != null) {
                try {
                    val output = editor.newOutputStream(0).buffered()
                    res.bitmap.compress(Bitmap.CompressFormat.PNG, 0, output)

                    output.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    editor.commit()
                }
            }
        }
    }

    suspend fun getResource(key: String): BitmapResource? {
        return withContext(Dispatchers.IO) {
            val snapshot = getLruCache().get(key)
            var result: BitmapResource? = null
            if (snapshot != null) {
                try {
                    val input = snapshot.getInputStream(0)
                    result = BitmapResource(input)
                    input.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    snapshot.close()
                }
            }
            result
        }
    }
}