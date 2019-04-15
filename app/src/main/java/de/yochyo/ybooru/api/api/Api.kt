package de.yochyo.ybooru.api.api

import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.database.database
import de.yochyo.ybooru.database.entities.Tag
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.isNotEmpty
import kotlin.collections.plusAssign
import kotlin.collections.sortBy

abstract class Api(var url: String) {

    companion object {
        val apis = ArrayList<Api>()
        fun addApi(api: Api) = apis.add(api)
        fun initApi(name: String, url: String): Api? {
            val api = apis.find { it.name.equals(name, true) }
            if (api != null) api.url = url
            instance = api
            return api
        }


        const val searchTagLimit = 10
        var instance: Api? = null
        val name: String
            get() = instance!!.name

        suspend fun searchTags(beginSequence: String): List<Tag> {
            val array = ArrayList<Tag>(searchTagLimit)
            val json = getJson(instance!!.urlGetTags(beginSequence))
            if (json != null) {
                for (i in 0 until json.length()) {
                    val tag = Api.instance!!.getTagFromJson(json.getJSONObject(i))
                    if (tag != null) array.add(tag)
                }
                array.sortBy { it.type }
            }
            return array
        }

        suspend fun getTag(name: String): Tag? {
            if (name == "*") return Tag(name, Tag.UNKNOWN)
            val json = getJson(instance!!.urlGetTag(name))
            if (json != null)
                if (!json.isNull(0))
                    return Api.instance!!.getTagFromJson(json.getJSONObject(0))
            return null
        }

        suspend fun getPosts(page: Int, tags: Array<String>, limit: Int = database.limit): List<Post> {
            var array = arrayOfNulls<Post>(limit)
            var url = instance!!.urlGetPosts(page, tags, limit)

            if (tags.filter { it != "" }.isNotEmpty()) {
                url += "&tags="
                for (tag in tags)
                    url += "$tag "
                url = url.substring(0, url.length - 1)
            }
            val json = getJson(url)

            if (json != null) {
                val root = GlobalScope.launch {
                    for (i in 0 until json.length()) {
                        val pos = i
                        launch {
                            val post = Api.instance!!.getPostFromJson(json.getJSONObject(i))
                            array[pos] = post
                        }
                    }
                }
                root.join()
                val filter = array.filter { (it != null && (it.extension == "png" || it.extension == "jpg")) } as List<Post>
                if (database.r18) return filter.filter { it.rating == "s" }
            }
            return array.filter { it != null } as List<Post>
        }

        suspend fun newestID(): Int {
            val json = getJson(instance!!.urlGetNewest())
            if (json?.getJSONObject(0) != null) {
                return json.getJSONObject(0).getInt("id")
            }
            return 0
        }

        @Suppress("BlockingMethodInNonBlockingContext")
        private suspend fun getJson(urlToRead: String): JSONArray? {
            println("Download JSON: $urlToRead")
            var array: JSONArray? = null
            try {
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
                        println("URL: $urlToRead")
                        e.printStackTrace()
                    }
                }
                job.join()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return array
        }
    }

    protected fun getURLSourceLines(url: String): ArrayList<String> {
        println("get html: $url")
        val urlObject = URL(url)
        val urlConnection = urlObject.openConnection() as HttpURLConnection
        urlConnection.addRequestProperty("User-Agent", "Mozilla/5.00")
        val inputStream = urlConnection.inputStream
        BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { bufferedReader ->
            var inputLine: String? = bufferedReader.readLine()
            val array = ArrayList<String>()
            while (inputLine != null) {
                array += inputLine
                inputLine = bufferedReader.readLine()
            }
            inputStream.close()
            return array
        }
    }

    abstract val name: String
    abstract fun urlGetTags(beginSequence: String): String
    abstract fun urlGetTag(name: String): String
    abstract fun urlGetPosts(page: Int, tags: Array<String>, limit: Int): String
    abstract fun urlGetNewest(): String

    abstract suspend fun getTagFromJson(json: JSONObject): Tag?
    abstract suspend fun getPostFromJson(json: JSONObject): Post?
}