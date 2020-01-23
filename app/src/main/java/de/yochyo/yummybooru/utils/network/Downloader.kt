package de.yochyo.yummybooru.utils.network

import android.content.Context
import de.yochyo.downloader.RegulatingDownloader
import de.yochyo.yummybooru.api.entities.Resource
import de.yochyo.yummybooru.utils.general.Logger
import de.yochyo.yummybooru.utils.general.cache
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.InputStream

class CacheableDownloader(maxThreads: Int) : RegulatingDownloader<Resource>(maxThreads) {
    override fun toResource(inputStream: InputStream, data: Any): Resource {
        return Resource(inputStream.readBytes(), data as Int)
    }

    override fun download(url: String, callback: suspend (e: Resource) -> Unit, downloadFirst: Boolean, data: Any) {
        try {
            super.download(url, callback, downloadFirst, Resource.getTypeFromURL(url))
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
            Logger.log(e, filePrefix = "OutOfMemory")
        }
    }

    fun downloadAndCache(context: Context, url: String, callback: suspend (e: Resource) -> Unit, downloadFirst: Boolean, id: String) {
        download(url, {
            GlobalScope.launch { context.cache.cacheFile(id, it) }
            callback(it)
        }, downloadFirst)
    }
}

val downloader = CacheableDownloader(5)

