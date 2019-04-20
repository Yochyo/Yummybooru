package de.yochyo.ybooru.api

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.concurrent.LinkedBlockingDeque

abstract class Downloader(context: Context) {
    private val path = "${context.cacheDir}/"
    private val directory = File(path)
    private val downloads = LinkedBlockingDeque<Download>()

    companion object {
        private var instance: Downloader? = null
        fun getInstance(context: Context): Downloader {
            if (instance == null) instance = object : Downloader(context) {}
            return instance!!
        }
    }

    init {
        for (i in 1..3) {
            GlobalScope.launch(Dispatchers.IO) {
                while (true) {
                    if (downloads.isNotEmpty()) {
                        try {
                            val download = downloads.takeLast()
                            var bitmap = getCachedBitmap(download.id)
                            if (bitmap == null) {
                                val conn = URL(download.url).openConnection()
                                conn.addRequestProperty("User-Agent", "Mozilla/5.00")
                                val stream = conn.getInputStream()
                                bitmap = BitmapFactory.decodeStream(stream)
                                stream.close()
                            }
                            if (download.cache) launch(Dispatchers.IO) { cacheBitmap(download.id, bitmap!!) }
                            launch(Dispatchers.Main) { download.doAfter.invoke(this, bitmap!!) }
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

    fun downloadImage(url: String, id: String, doAfter: suspend CoroutineScope.(bitmap: Bitmap) -> Unit = {}, downloadNow: Boolean = false, cache: Boolean = true) {
        val download = Download(url, id, cache, doAfter)
        if (downloadNow) downloads.putLast(download)
        else downloads.putFirst(download)
    }

    fun getCachedFile(id: String): File? {
        val f = File(id)
        if (f.exists()) return f
        return null
    }

    fun getCachedBitmap(id: String): Bitmap? {
        val f = getCachedFile(id)?.apply {
            val stream = inputStream()
            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            return bitmap
        }
        return null
    }

    private fun cacheBitmap(id: String, bitmap: Bitmap) {
        val f = file(id)
        try {
            if (!f.exists()) {
                f.createNewFile()
                val output = ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }
                f.writeBytes(output.toByteArray())
                output.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeCachedBitmap(id: String) {
        try {
            file(id).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearCache() {
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

    private fun file(id: String) = File("$path$id")
}

val Context.downloader: Downloader get() = Downloader.getInstance(this)
fun Context.downloadImage(url: String, id: String, doAfter: suspend CoroutineScope.(bitmap: Bitmap) -> Unit = {}, downloadNow: Boolean = false, cache: Boolean = true) = Downloader.getInstance(this).downloadImage(url, id, doAfter, downloadNow, cache)


private class Download(val url: String, val id: String, val cache: Boolean, val doAfter: suspend CoroutineScope.(bitmap: Bitmap) -> Unit = {})