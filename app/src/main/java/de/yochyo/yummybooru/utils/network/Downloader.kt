package de.yochyo.yummybooru.utils.network

import de.yochyo.downloader.RegulatingDownloader
import de.yochyo.yummybooru.api.entities.Resource
import de.yochyo.yummybooru.utils.general.Logger
import de.yochyo.yummybooru.utils.general.cache
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream

class CacheableDownloader(maxThreads: Int) {
    private val dl = object : RegulatingDownloader<Resource>(maxThreads) {
        override fun toResource(inputStream: InputStream, data: Any): Resource {
            return Resource(inputStream.readBytes(), data as Int)
        }
    }

    fun download(url: String, id: String, callback: suspend (e: Resource) -> Unit, downloadFirst: Boolean = false, cacheFile: Boolean = false) {
        suspend fun doAfter(res: Resource?) {
            if (res != null) {
                if (cacheFile) GlobalScope.launch { cache.cacheFile(id, res) }
                callback(res)
            }
        }
        try {
            GlobalScope.launch {
                val res = cache.getCachedFile(id)
                if (res != null) doAfter(res)
                else dl.download(url, { doAfter(it) }, downloadFirst, Resource.getTypeFromURL(url))
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            Logger.log(e, filePrefix = "OutOfMemory")
        }
    }
}

val downloader = CacheableDownloader(5)
fun download(url: String, id: String, callback: suspend (e: Resource) -> Unit, downloadFirst: Boolean = false, cacheFile: Boolean = false) = downloader.download(url, id, callback, downloadFirst, cacheFile)

