package de.yochyo.yummybooru.api.api

import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

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
                    val tag = instance!!.getTagFromJson(json.getJSONObject(i))
                    if (tag != null) array.add(tag)
                }
            }
            return array
        }

        suspend fun getTag(name: String): Tag {
            if (name == "*") return Tag(name, Tag.UNKNOWN, count = newestID())
            val json = getJson(instance!!.urlGetTag(name))
            if (json != null && json.length() > 0) {
                val tag = instance!!.getTagFromJson(json.getJSONObject(0))
                if (tag != null) return tag
            }
            return Tag(name, Tag.UNKNOWN)
        }

        suspend fun getPosts(page: Int, tags: Array<String>, limit: Int = db.limit): List<Post> {
            val array = ArrayList<Post>(limit)
            var url = instance!!.urlGetPosts(page, tags, limit)

            if (tags.isNotEmpty()) {
                url += "&tags="
                for (tag in tags)
                    url += "$tag "
                url = url.substring(0, url.length - 1)
            }
            val json = getJson(url)

            if (json != null) {
                for (i in 0 until json.length()) {
                    val post = instance!!.getPostFromJson(json.getJSONObject(i))
                    if (post != null) array += post
                }
                if (Server.currentServer.enableR18Filter) return array.filter { it.rating == "s" }
            }
            return array
        }

        suspend fun newestID(): Int {
            val json = getJson(instance!!.urlGetPosts(1, arrayOf("*"), 1))
            return json?.getJSONObject(0)?.getInt("id") ?: 0
        }

        private suspend fun getJson(urlToRead: String): JSONArray? {
            var array: JSONArray? = null
            withContext(Dispatchers.IO) {
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
                    Logger.log(e, "URL: $urlToRead")
                }
            }
            return array
        }
    }

    protected suspend fun getURLSourceLines(url: String): ArrayList<String> {
        return withContext(Dispatchers.IO) {
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

                array
            }
        }
    }

    abstract val name: String
    abstract fun urlGetTags(beginSequence: String): String
    abstract fun urlGetTag(name: String): String
    abstract fun urlGetPosts(page: Int, tags: Array<String>, limit: Int): String

    abstract fun getTagFromJson(json: JSONObject): Tag?
    abstract fun getPostFromJson(json: JSONObject): Post?
}

val api: Api get() = Api.instance!!