package de.yochyo.yummybooru.api.downloads

import android.content.Context
import de.yochyo.yummybooru.utils.network.DownloadUtils
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingDeque

abstract class Downloader(context: Context) {
    companion object {
        private var _instance: Downloader? = null
        fun getInstance(context: Context): Downloader {
            if (_instance == null) _instance = object : Downloader(context) {}
            return _instance!!
        }

    }

    private val downloads = LinkedBlockingDeque<Download>()

    init {
        for (i in 1..5) {
            GlobalScope.launch(Dispatchers.IO) {
                while (true) {
                    if (downloads.isNotEmpty()) {
                        var download: Download? = null
                        try {
                            download = downloads.takeLast()
                            var byteArray = context.cache.getCachedFile(download.id)
                            if (byteArray == null) {
                                byteArray = DownloadUtils.downloadByteArray(download.url)!! //throws exception when null
                                if (download.cache) GlobalScope.launch { context.cache.cacheFile(download.id, byteArray) }
                            }
                            launch(Dispatchers.Main) { download.doAfter.invoke(this, byteArray) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        delay(5)
                    } else
                        delay(50)
                }
            }
        }
    }

    fun downloadImage(url: String, id: String, doAfter: suspend CoroutineScope.(image: ByteArray) -> Unit = {}, downloadNow: Boolean = true, cache: Boolean = true) {
        if (downloads.none { it.url == url }) {
            val download = Download(url, id, cache, doAfter)
            if (downloadNow) downloads.putLast(download)
            else downloads.putFirst(download)
        }
    }
}

inline val Context.downloader: Downloader get() = Downloader.getInstance(this)
fun Context.downloadImage(url: String, id: String, doAfter: suspend CoroutineScope.(image: ByteArray) -> Unit = {}, downloadNow: Boolean = true, cache: Boolean = true) = Downloader.getInstance(this).downloadImage(url, id, doAfter, downloadNow, cache)


private class Download(val url: String, val id: String, val cache: Boolean, val doAfter: suspend CoroutineScope.(image: ByteArray) -> Unit = {})