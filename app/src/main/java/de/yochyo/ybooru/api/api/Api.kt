package de.yochyo.ybooru.api.api

import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.database.database
import de.yochyo.ybooru.database.entities.Tag
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

abstract class Api(var url: String) {

    companion object {
        private val apis = ArrayList<Api>()
        fun addApi(api: Api) = apis.add(api)
        fun initApi(name: String, url: String): Api? {
            println(apis.joinToString { it.name })
            println(name)
            val api = apis.find { it.name.equals(name, true) }
            println(api)
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
                    val tag = Tag.getTagFromJson(json.getJSONObject(i))
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
                    return Tag.getTagFromJson(json.getJSONObject(0))
            return null
        }

        suspend fun getPosts(page: Int, tags: Array<String>, limit: Int = database.limit): List<Post> {
            var array: List<Post> = ArrayList(limit)
            var url = instance!!.urlGetPosts(page, tags, limit)

            if (tags.filter { it != "" }.isNotEmpty()) {
                url += "&tags="
                for (tag in tags)
                    url += "$tag "
                url = url.substring(0, url.length - 1)
            }
            val json = getJson(url)
            if (json != null) {
                for (i in 0 until json.length()) {
                    val post = Post.getPostFromJson(json.getJSONObject(i))
                    if (post != null)
                        (array as ArrayList<Post>) += post
                }
                array = array.filter { it.extension == "png" || it.extension == "jpg" || it.extension == "jpeg" }
                if (database.r18) return array.filter { it.rating == "s" }
            }
            return array
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

    abstract val name: String
    abstract fun urlGetTags(beginSequence: String): String
    abstract fun urlGetTag(name: String): String
    abstract fun urlGetPosts(page: Int, tags: Array<String>, limit: Int): String
    abstract fun urlGetNewest(): String
}