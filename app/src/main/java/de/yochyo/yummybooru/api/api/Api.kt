package de.yochyo.yummybooru.api.api

import android.content.Context
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.parseUrlCharacters
import de.yochyo.yummybooru.utils.network.DownloadUtils
import org.json.JSONObject
import java.net.URLEncoder

abstract class Api(var url: String) {

    companion object {
        const val searchTagLimit = 10

        private var instance: Api? = null

        val apis = ArrayList<Api>()
        init{
            apis.add(DanbooruApi(""))
            apis.add(MoebooruApi(""))
        }

        fun addApi(api: Api) = apis.add(api)

        fun getApi(context: Context): Api{
            if(instance == null){
                val server = Server.getCurrentServer(context)
                selectApi(server.name, server.url)
            }
            return instance!!
        }

        fun selectApi(name: String, url: String): Api? {
            val api = apis.find { it.name.equals(name, true) }
            if (api != null) api.url = url
            instance = api
            return api
        }


        suspend fun getTags(context: Context, beginSequence: String): List<Tag> {
            val array = ArrayList<Tag>(searchTagLimit)
            val json = DownloadUtils.getJson(context.api.urlGetTags(context, parseUrlCharacters(beginSequence)))
            if (json != null) {
                for (i in 0 until json.length()) {
                    val tag = context.api.getTagFromJson(context, json.getJSONObject(i))
                    if (tag != null) array.add(tag)
                }
            }
            return array
        }

        suspend fun getTag(context: Context, name: String): Tag {
            if (name == "*") return Tag(context, name, Tag.SPECIAL, count = newestID(context))
            val json = DownloadUtils.getJson(context.api.urlGetTag(context, parseUrlCharacters(name)))
            if (json != null && json.length() > 0) {
                val tag = context.api.getTagFromJson(context, json.getJSONObject(0))
                if (tag != null) return tag
            }
            return Tag(context, name, if (Tag.isSpecialTag(name)) Tag.SPECIAL else Tag.UNKNOWN)
        }

        suspend fun getPosts(context: Context, page: Int, tags: Array<String>, limit: Int = context.db.limit): List<Post>? {
            val posts = ArrayList<Post>(limit)
            val urlBuilder = StringBuilder().append(context.api.urlGetPosts(context, page, tags, limit))
            if (tags.isNotEmpty()) {
                urlBuilder.append("&tags=")
                for (tag in tags)
                    urlBuilder.append("${URLEncoder.encode(tag, "UTF-8")} ")
                urlBuilder.deleteCharAt(urlBuilder.lastIndex)
            }

            val json = DownloadUtils.getJson(urlBuilder.toString())
            if (json != null) {
                for (i in 0 until json.length()) {
                    val post = context.api.getPostFromJson(context, json.getJSONObject(i))
                    if (post != null) posts += post
                }
            } else return null
            return if (Server.getCurrentServer(context).enableR18Filter) posts.filter { it.rating == "s" }
            else posts
        }

        suspend fun newestID(context: Context): Int {
            val json = DownloadUtils.getJson(context.api.urlGetPosts(context, 1, arrayOf("*"), 1))
            return json?.getJSONObject(0)?.getInt("id") ?: 0
        }

    }

    abstract val name: String
    abstract fun urlGetTags(context: Context, beginSequence: String): String
    abstract fun urlGetTag(context: Context, name: String): String
    abstract fun urlGetPosts(context: Context, page: Int, tags: Array<String>, limit: Int): String

    abstract fun getTagFromJson(context: Context, json: JSONObject): Tag?
    abstract fun getPostFromJson(context: Context, json: JSONObject): Post?
}

val Context.api: Api get() = Api.getApi(this)