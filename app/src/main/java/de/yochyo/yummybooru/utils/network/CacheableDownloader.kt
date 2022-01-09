package de.yochyo.yummybooru.utils.network

import android.content.Context
import de.yochyo.booruapi.api.Post
import de.yochyo.downloader.RegulatingDownloader
import de.yochyo.yummybooru.api.entities.BitmapResource
import de.yochyo.yummybooru.api.entities.Resource2
import de.yochyo.yummybooru.utils.cache.cache
import de.yochyo.yummybooru.utils.general.mimeType
import de.yochyo.yummybooru.utils.general.original
import de.yochyo.yummybooru.utils.general.preview
import de.yochyo.yummybooru.utils.general.sample
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream

class CacheableDownloader(max: Int) {
    val dl = object : RegulatingDownloader<Resource2>(max) {
        init {
            config.closeStreamAfterDownload = false
        }

        override fun toResource(inputStream: InputStream, context: Any): Resource2 {
            return Resource2(context as String, inputStream)
        }
    }

    fun download(url: String, callback: suspend (e: Resource2?) -> Unit, headers: Map<String, String>, downloadNow: Boolean = false) {
        if (downloadNow) dl.downloadNow(url, callback, headers, url.mimeType ?: "")
        else dl.download(url, callback, headers, url.mimeType ?: "")
    }

    suspend fun downloadSync(url: String, headers: Map<String, String>) = dl.downloadSync(url, headers, url.mimeType ?: "")

    fun downloadPostPreviewIMG(context: Context, post: Post, callback: suspend (e: BitmapResource?) -> Unit, headers: Map<String, String>, downloadFirst: Boolean = false) {
        return downloadBitmap(context, post.filePreviewURL, context.preview(post.id), callback, headers, downloadFirst)
    }

    fun downloadPostSampleIMG(context: Context, post: Post, callback: suspend (e: BitmapResource?) -> Unit, headers: Map<String, String>, downloadFirst: Boolean = false) {
        return downloadBitmap(context, post.fileSampleURL, context.sample(post.id), callback, headers, downloadFirst)
    }

    fun downloadPostOriginalIMG(context: Context, post: Post, callback: suspend (e: BitmapResource?) -> Unit, headers: Map<String, String>, downloadFirst: Boolean = false) {
        return downloadBitmap(context, post.fileURL, context.original(post.id), callback, headers, downloadFirst)
    }

    private fun downloadBitmap(
        context: Context,
        url: String,
        id: String,
        callback: suspend (e: BitmapResource?) -> Unit,
        headers: Map<String, String>,
        downloadFirst: Boolean = false
    ) {
        suspend fun doAfter(res: BitmapResource?) {
            if (res != null) {
                GlobalScope.launch { context.cache.cache(id, res) }
            }
            callback(res)
        }

        try {
            GlobalScope.launch {
                val res = context.cache.getResource(id)
                when {
                    res != null -> doAfter(res)
                    downloadFirst -> dl.downloadNow(url, { doAfter(BitmapResource.from(it)) }, headers, url.mimeType ?: "")
                    else -> dl.download(url, { doAfter(BitmapResource.from(it)) }, headers, url.mimeType ?: "")
                }
            }
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        }
    }
}

val downloader = CacheableDownloader(10)
