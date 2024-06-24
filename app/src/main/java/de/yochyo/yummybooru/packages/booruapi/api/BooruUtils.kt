package de.yochyo.booruapi.api

import de.yochyo.json.JSONArray
import de.yochyo.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

open class BooruUtils(
    var defaultHeaders: Map<String, String> =
        mapOf(Pair("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/106.0.0.0 Safari/537.36"))
) {

    /**
     * Returns a JSONObject parsed from an url
     * @param urlToRead url to read to Object from
     * @return JSONObject or null if error
     */
    suspend fun getJsonObjectFromUrl(urlToRead: String): JSONObject? {
        var json: String? = null
        return try {
            json = String(getUrlInputStream(urlToRead)!!.use { it.readBytes() })
            JSONObject(json)
        } catch (e: Exception) {
            e.printStackTrace()
            println(json)
            null
        }
    }

    /**
     * Returns a JSONArray parsed from an url
     * @param urlToRead url to read to Array from
     * @return JSONArray or null if error
     */
    suspend fun getJsonArrayFromUrl(urlToRead: String): JSONArray? {
        var json: String? = null
        return try {
            json = String(getUrlInputStream(urlToRead)!!.use { it.readBytes() })
            JSONArray(json)
        } catch (e: Exception) {
            e.printStackTrace()
            println(json)
            null
        }
    }

    /**
     * Returns a String parsed from an url
     * @param urlToRead url to read to String from
     * @return String or null if error
     */
    suspend fun getStringFromUrl(urlToRead: String): String? {
        return try {
            String(getUrlInputStream(urlToRead)!!.use { it.readBytes() })
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns an Inputstream from an url
     * @param urlToRead Inputstream to the Url
     * @return Inputstream or null if error
     */
    suspend fun getUrlInputStream(url: String): InputStream? {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                val conn = URL(url).openConnection() as HttpURLConnection
                for (header in getHeaders()) conn.addRequestProperty(header.key, header.value)
                conn.requestMethod = "GET"
                val input = conn.inputStream
                input
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Returns a String Array made with "bufferedReader.readLine()" parsed from an url.
     * @param urlToRead url to read to String Array from
     * @return String Array or null if error
     */
    suspend fun getUrlSource(urlToRead: String): List<String> {
        return withContext(Dispatchers.IO) {
            val list = LinkedList<String>()
            try {
                val stream = getUrlInputStream(urlToRead)
                if (stream != null) {
                    BufferedReader(InputStreamReader(stream, "UTF-8")).use { bufferedReader ->
                        var inputLine: String? = bufferedReader.readLine()
                        while (inputLine != null) {
                            list += inputLine
                            inputLine = bufferedReader.readLine()
                        }
                        stream.close()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            list
        }
    }

    open fun getHeaders(): Map<String, String> {
        return defaultHeaders
    }
}
