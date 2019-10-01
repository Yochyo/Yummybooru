package de.yochyo.yummybooru.utils.network

import de.yochyo.yummybooru.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object DownloadUtils{
    suspend fun getUrlLines(urlToRead: String): Collection<String> {
        return withContext(Dispatchers.IO) {
            val result = ArrayList<String>()
            try {
                val conn = URL(urlToRead).openConnection() as HttpURLConnection
                conn.addRequestProperty("User-Agent", "Mozilla/5.00")
                conn.requestMethod = "GET"
                val stream= conn.inputStream
                BufferedReader(InputStreamReader(stream, "UTF-8")).use { bufferedReader ->
                    var inputLine: String? = bufferedReader.readLine()
                    while (inputLine != null) {
                        result.add(inputLine)
                        inputLine = bufferedReader.readLine()
                    }
                    stream.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.log(e, "URL: $urlToRead")
            }
            result
        }
    }

    suspend fun getJson(urlToRead: String): JSONArray?{
        var array: JSONArray?=null
        try {
            array = JSONArray(getUrlLines(urlToRead).joinToString(""))
        }catch (e: Exception){
            e.printStackTrace()
            Logger.log(e, "URL: $urlToRead")
        }
        return array
    }

    //TODO Api weiter optimieren, loggs schreiben, DownloadUtils verkürzen
}