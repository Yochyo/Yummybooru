package de.yochyo.yummybooru.utils.network

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import de.yochyo.yummybooru.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object DownloadUtils{

    suspend fun getUrlInputStream(url: String): InputStream?{
        return withContext(Dispatchers.IO){
            return@withContext try{
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.addRequestProperty("User-Agent", "Mozilla/5.00")
                conn.requestMethod = "GET"
                conn.inputStream
            }catch(e: Exception){
                e.printStackTrace()
                Logger.log(e, url)
                null
            }
        }
    }
    suspend fun getUrlLines(urlToRead: String): Collection<String> {
        return withContext(Dispatchers.IO) {
            val result = ArrayList<String>()
            try {
                val stream = getUrlInputStream(urlToRead)
                if(stream != null){
                    BufferedReader(InputStreamReader(stream, "UTF-8")).use { bufferedReader ->
                        var inputLine: String? = bufferedReader.readLine()
                        while (inputLine != null) {
                            result.add(inputLine)
                            inputLine = bufferedReader.readLine()
                        }
                        stream.close()
                    }
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
    suspend fun downloadBitmap(url: String): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val stream = DownloadUtils.getUrlInputStream(url)
                if(stream != null){
                    val bitmap = BitmapFactory.decodeStream(stream)
                    stream.close()
                    return@withContext bitmap
                }
            }catch (e: Exception){
                Logger.log(e, url)
                e.printStackTrace()
            }
            null
        }
    }

    //TODO Api weiter optimieren, loggs schreiben, DownloadUtils verkürzen
}