package de.yochyo.downloader

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

object DownloadUtils {
    suspend fun getUrlInputStream(url: String, headers: Map<String, String>): InputStream? {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                val conn = URL(url).openConnection() as HttpURLConnection
                if (!headers.keys.contains("User-Agent"))
                    conn.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:84.0) Gecko/20100101 Firefox/84.0")
                for (header in headers) conn.addRequestProperty(header.key, header.value)
                conn.requestMethod = "GET"
                val input = conn.inputStream
                input
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}