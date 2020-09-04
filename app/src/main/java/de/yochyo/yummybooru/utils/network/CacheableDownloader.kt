package de.yochyo.yummybooru.utils.network

import android.content.Context
import de.yochyo.downloader.RegulatingDownloader
import de.yochyo.yummybooru.api.entities.Resource
import de.yochyo.yummybooru.utils.cache.cache
import de.yochyo.yummybooru.utils.general.log
import de.yochyo.yummybooru.utils.general.mimeType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream

class CacheableDownloader(max: Int) {
    val dl = object : RegulatingDownloader<Resource>(max) {
        override fun toResource(inputStream: InputStream, context: Any): Resource {
            return Resource(inputStream.readBytes(), context as String)
        }
    }

    fun download(url: String, callback: suspend (e: Resource) -> Unit) = dl.download(url, callback, url.mimeType ?: "")
    fun download(context: Context, url: String, id: String, callback: suspend (e: Resource) -> Unit, downloadFirst: Boolean = false, cacheFile: Boolean = false) {
        suspend fun doAfter(res: Resource?) {
            if (res != null) {
                if (cacheFile) GlobalScope.launch { context.cache.cache(id, res) }
                callback(res)
            }
        }
        try {
            GlobalScope.launch {
                val res = context.cache.getResource(id)
                when {
                    res != null -> doAfter(res)
                    downloadFirst -> dl.downloadNow(url, { doAfter(it) }, url.mimeType ?: "")
                    else -> dl.download(url, { doAfter(it) }, url.mimeType ?: "")
                }
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            e.log()
        }
    }
}

val downloader = CacheableDownloader(10)
fun download(context: Context, url: String, id: String, callback: suspend (e: Resource) -> Unit, downloadFirst: Boolean = false, cacheFile: Boolean = false) =
    downloader.download(context, url, id, callback, downloadFirst, cacheFile)

