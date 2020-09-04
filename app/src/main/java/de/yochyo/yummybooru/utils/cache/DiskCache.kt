package de.yochyo.yummybooru.utils.cache

import com.jakewharton.disklrucache.DiskLruCache
import de.yochyo.yummybooru.api.entities.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class DiskCache(private val dir: File) {
    private var _diskLruCache: DiskLruCache? = null
    private val lock = Mutex()

    private suspend fun getLruCache(): DiskLruCache {
        return withContext(Dispatchers.IO) {
            lock.withLock {
                if (_diskLruCache == null) _diskLruCache = DiskLruCache.open(dir, 1, 1, 1024 * 1024 * 512)
                _diskLruCache!!
            }
        }
    }

    suspend fun cacheResource(key: String, res: Resource) {
        withContext(Dispatchers.IO) {
            val editor = getLruCache().edit(key)
            if (editor != null) {
                try {
                    val outputStream = BufferedOutputStream(editor.newOutputStream(0))
                    val objectOutputStream = ObjectOutputStream(outputStream)
                    objectOutputStream.writeObject(res)

                    objectOutputStream.close()
                    outputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    editor.commit()
                }
            }
        }
    }

    suspend fun getResource(key: String): Resource? {
        return withContext(Dispatchers.IO) {
            val snapshot = getLruCache().get(key)
            var result: Resource? = null
            if (snapshot != null) {
                try {
                    val inputStream = snapshot.getInputStream(0)
                    val objectInputStream = ObjectInputStream(inputStream)
                    val obj = objectInputStream.readObject()
                    objectInputStream.close()
                    inputStream.close()
                    result = obj as Resource
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