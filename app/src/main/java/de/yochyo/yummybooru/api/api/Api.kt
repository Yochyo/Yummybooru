package de.yochyo.yummybooru.api.api

import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.network.DownloadUtils
import org.json.JSONObject

abstract class Api(var url: String) {

    companion object {
        const val searchTagLimit = 10

        var instance: Api? = null

        val apis = ArrayList<Api>()
        fun addApi(api: Api) = apis.add(api)

        fun selectApi(name: String, url: String): Api? {
            val api = apis.find { it.name.equals(name, true) }
            if (api != null) api.url = url
            instance = api
            return api
        }


        suspend fun getTags(beginSequence: String): List<Tag> {
            val array = ArrayList<Tag>(searchTagLimit)
            val json = DownloadUtils.getJson(api.urlGetTags(beginSequence))
            if (json != null) {
                for (i in 0 until json.length()) {
                    val tag = api.getTagFromJson(json.getJSONObject(i))
                    if (tag != null) array.add(tag)
                }
            }
            return array
        }

        suspend fun getTag(name: String): Tag {
            if (name == "*") return Tag(name, Tag.SPECIAL, count = newestID())
            val json = DownloadUtils.getJson(api.urlGetTag(name))
            if (json != null && json.length() > 0) {
                val tag = api.getTagFromJson(json.getJSONObject(0))
                if (tag != null) return tag
            }
            return Tag(name, if (Tag.isSpecialTag(name)) Tag.SPECIAL else Tag.UNKNOWN)
        }

        suspend fun getPosts(page: Int, tags: Array<String>, limit: Int = db.limit): List<Post> {
            val posts = ArrayList<Post>(limit)
            val urlBuilder = StringBuilder().append(api.urlGetPosts(page, tags, limit))
            if (tags.isNotEmpty()) {
                urlBuilder.append("&tags=")
                for (tag in tags)
                    urlBuilder.append("$tag ")
                urlBuilder.deleteCharAt(urlBuilder.lastIndex)
            }

            val json = DownloadUtils.getJson(urlBuilder.toString())
            if (json != null) {
                for (i in 0 until json.length()) {
                    val post = api.getPostFromJson(json.getJSONObject(i))
                    if (post != null) posts += post
                }
            }
            return if (Server.currentServer.enableR18Filter) posts.filter { it.rating == "s" }
            else posts
        }

        suspend fun newestID(): Int {
            val json = DownloadUtils.getJson(api.urlGetPosts(1, arrayOf("*"), 1))
            return json?.getJSONObject(0)?.getInt("id") ?: 0
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