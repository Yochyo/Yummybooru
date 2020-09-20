package de.yochyo.yummybooru.utils.network

import android.content.Context
import de.yochyo.downloader.RegulatingDownloader
import de.yochyo.yummybooru.api.entities.BitmapResource
import de.yochyo.yummybooru.api.entities.Resource2
import de.yochyo.yummybooru.utils.cache.cache
import de.yochyo.yummybooru.utils.general.log
import de.yochyo.yummybooru.utils.general.mimeType
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream

class CacheableDownloader(max: Int) {
    val dl = object : RegulatingDownloader<Resource2>(max) {
        override fun toResource(inputStream: InputStream, context: Any): Resource2 {
            return Resource2(context as String, inputStream)
        }
    }

    fun download(url: String, callback: suspend (e: Resource2) -> Unit, downloadNow: Boolean = false) {
        if (downloadNow) dl.downloadNow(url, callback, url.mimeType ?: "")
        else dl.download(url, callback, url.mimeType ?: "")
    }

    fun downloadBitmap(context: Context, url: String, id: String, callback: suspend (e: BitmapResource) -> Unit, downloadFirst: Boolean = false) {
        suspend fun doAfter(res: BitmapResource?) {
            if (res != null) {
                GlobalScope.launch { context.cache.cache(id, res) }
                callback(res)
            }
        }
        try {
            GlobalScope.launch {
                val res = context.cache.getResource(id)
                when {
                    res != null -> doAfter(res)
                    downloadFirst -> dl.downloadNow(url, { doAfter(BitmapResource.from(it)) }, url.mimeType ?: "")
                    else -> dl.download(url, { doAfter(BitmapResource.from(it)) }, url.mimeType ?: "")
                }
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            e.log()
        }
    }
}

val downloader = CacheableDownloader(10)
fun downloadBitmap(context: Context, url: String, id: String, callback: suspend (e: BitmapResource) -> Unit, downloadFirst: Boolean = false, cacheFile: Boolean = false) =
    downloader.downloadBitmap(context, url, id, callback, downloadFirst)

fun download(url: String, callback: suspend (e: Resource2) -> Unit, downloadNow: Boolean = false) = downloader.download(url, callback, downloadNow)