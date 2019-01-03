package de.yochyo.danbooruAPI

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import de.yochyo.yBooru.api.Post
import de.yochyo.yBooru.cache
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


object Api {
    val limit = 30
    var safeSearch: Boolean = true
    private val downloading = ArrayList<String>(20)


    suspend fun downloadImage(url: String, id: String): Bitmap {
        if (downloading.contains(id))
            return cache.awaitPicture(id)
        else {
            downloading += id
            val bitmap = BitmapFactory.decodeStream(URL(url).openStream()).apply { cache.cacheBitmap(id, this) }
            downloading -= id
            return bitmap
        }
    }
    suspend fun getPosts(page: Int, vararg tags: String): List<Post> {//TODO rating ist safeSearch
        var url = "https://danbooru.donmai.us/posts.json?limit=$limit&page=$page"
        if (tags.isNotEmpty()) {
            url += "&tags="
            for (tag in tags)
                url += "$tag "
            url = url.substring(0, url.length - 1)
        }
        val json = getJson(url)
        val array = ArrayList<Post>(limit)
        for (i in 0 until json.length()) {
            val post = Post.getPostFromJson(json.getJSONObject(i))
            if (post != null)
                array += post
        }
        if(safeSearch) return array.filter { it.rating == "s" }
        else return array
    }

    suspend private fun getJson(urlToRead: String): JSONArray {
        var array: JSONArray? = null
        val job = GlobalScope.async {
            val result = StringBuilder()
            val url = URL(urlToRead)
            val conn = url.openConnection() as HttpURLConnection
            conn.addRequestProperty("User-Agent", "Mozilla/5.00")
            conn.requestMethod = "GET"
            val rd = BufferedReader(InputStreamReader(conn.inputStream))
            var line: String? = rd.readLine()
            while (line != null) {
                result.append(line)
                line = rd.readLine()
            }
            rd.close()
            array = JSONArray(result.toString())
        }
        job.join()
        return array!!
    }

}