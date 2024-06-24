package de.yochyo.downloader

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.LinkedBlockingDeque


data class Download<E>(val url: String, val headers: Map<String, String>, val callback: suspend (e: E?) -> Unit, val data: Any)

@Suppress("BlockingMethodInNonBlockingContext")
abstract class ADownloader<E> : IDownloader<E> {
    val config = DownloaderConfig()
    protected val downloads = LinkedBlockingDeque<Download<E>>()

    abstract fun toResource(inputStream: InputStream, context: Any): E

    protected abstract fun keepCoroutineAliveWhile(scope: CoroutineScope): Boolean


    protected open fun onStartCoroutine() {}
    protected open fun onStopCoroutine() {}

    protected open suspend fun onDownloadedResource(e: E) {}
    protected open fun onAddDownload() {}

    override fun download(url: String, callback: suspend (e: E?) -> Unit, headers: Map<String, String>, context: Any) {
        downloads.putLast(Download(url, headers, callback, context))
        onAddDownload()
    }

    override fun downloadNow(url: String, callback: suspend (e: E?) -> Unit, headers: Map<String, String>, context: Any) {
        downloads.putFirst(Download(url, headers, callback, context))
        onAddDownload()
    }

    override suspend fun downloadSync(url: String, headers: Map<String, String>, context: Any): E? {
        return processNextFile(Download(url, headers, {}, context))
    }

    internal fun startCoroutine() {
        onStartCoroutine()
        GlobalScope.launch(Dispatchers.IO) {
            while (keepCoroutineAliveWhile(this)) {
                try {
                    processNextFile()
                } catch (e: Exception) {
                }
            }
            joinAll()
            onStopCoroutine()
        }
    }

    internal suspend fun processNextFile(download: Download<E> = downloads.takeLast()): E? {
        return withContext(Dispatchers.IO) {
            try {
                val stream = DownloadUtils.getUrlInputStream(download.url, download.headers)
                    ?: throw Exception("Could not find file at {${download.url}}")
                val result = toResource(stream, download.data)
                if (config.closeStreamAfterDownload)
                    stream.close()
                onDownloadedResource(result)
                launch { download.callback(result) }
                result
            } catch (e: Exception) {
                launch { download.callback(null) }
                e.printStackTrace()
                null
            }
        }
    }

}