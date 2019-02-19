package de.yochyo.ybooru.api

import android.content.Context
import de.yochyo.ybooru.database
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL


object Api {
    private val searchTagLimit = 10

    suspend fun searchTags(beginSequence: String): List<Tag> {
        val url = "https://danbooru.donmai.us/tags.json?search[name_matches]=$beginSequence*&limit=$searchTagLimit"
        val json = getJson(url)
        var array = ArrayList<Tag>(searchTagLimit)
        for (i in 0 until json.length()) {
            val tag = Tag.getTagFromJson(json.getJSONObject(i))
            if (tag != null) array.add(tag)
        }
        array.sortBy { it.type }
        return array
    }

    suspend fun getTag(name: String): Tag? {
        val url = "https://danbooru.donmai.us/tags.json?search[name_matches]=$name"
        val json = getJson(url)
        if (!json.isNull(0)) return Tag.getTagFromJson(json.getJSONObject(0))
        else return null
    }

    suspend fun getPosts(context: Context, page: Int, vararg tags: String): List<Post> {//TODO rating ist safeSearch
        var url = "https://danbooru.donmai.us/posts.json?limit=${context.database.limit}&page=$page"
        if (tags.filter { it != "" }.isNotEmpty()) {
            url += "&tags="
            for (tag in tags)
                url += "$tag "
            url = url.substring(0, url.length - 1)
        }
        val json = getJson(url)
        var array: List<Post> = ArrayList(context.database.limit)
        for (i in 0 until json.length()) {
            val post = Post.getPostFromJson(json.getJSONObject(i))
            if (post != null)
                array += post
        }
        array = array.filter { it.extension == "png" || it.extension == "jpg" || it.extension == "jpeg" }
        if (context.database.r18) return array.filter { it.rating == "s" }
        else return array
    }

    private suspend fun getJson(urlToRead: String): JSONArray {
        var array: JSONArray? = null
        val job = GlobalScope.launch {
            try {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        job.join()
        return array!!
    }

}