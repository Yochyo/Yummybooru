package de.yochyo.yummybooru.api.api

import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.utils.Logger
import de.yochyo.yummybooru.utils.network.DownloadUtils
import org.json.JSONObject

class MoebooruApi(url: String) : Api(url) {

    override val name: String = "moebooru"
    override fun urlGetTag(name: String): String = "${url}tag.json?name=$name*"
    override fun urlGetTags(beginSequence: String): String {
        return "${url}tag.json?name=$beginSequence*&limit=$searchTagLimit&search[order]=count"
    }

    override fun urlGetPosts(page: Int, tags: Array<String>, limit: Int): String {
        return "${url}post.json?limit=$limit&page=$page&login=${Server.currentServer.userName}&password_hash=${Server.currentServer.passwordHash}"
    }

    override fun getPostFromJson(json: JSONObject): Post? {
        try {
            val fileURL = json.getString("file_url")
            val id = json.getInt("id")

            return object : Post() {
                override val id = id
                override val width = json.getInt("jpeg_width")
                override val height = json.getInt("jpeg_height")
                override val extension = fileURL.substring(fileURL.lastIndexOf(".") + 1)
                override val rating = json.getString("rating")
                override val fileSize = json.getInt("file_size")
                override val fileURL = fileURL
                override val fileSampleURL = json.getString("sample_url")
                override val filePreviewURL = json.getString("preview_url")
                private var tags: List<Tag>? = null
                override suspend fun getTags(): List<Tag> {
                    if (tags == null)
                        tags = parseTagsfromURL(DownloadUtils.getUrlLines("${Server.currentServer.url}post/show/$id")).sortedBy {
                            when(it.type){
                                Tag.ARTIST -> 0
                                Tag.COPYPRIGHT -> 1
                                Tag.CHARACTER -> 2
                                Tag.GENERAL -> 3
                                Tag.META -> 4
                                else -> 5
                            }
                        }
                    return tags!!
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.log(e, json.toString())
            return null
        }
    }

    override fun getTagFromJson(json: JSONObject): Tag? {
        return try {
            val name = json.getString("name")
            Tag(name, json.getInt("type"), count = json.getInt("count"))
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.log(e, json.toString())
            null
        }
    }

    private fun parseTagsfromURL(lines: Collection<String>): List<Tag> {
        val tags = ArrayList<Tag>()
        fun getCurrentTagType(type: String): Int {
            if (type.contains("tag-type-general")) return Tag.GENERAL
            if (type.contains("tag-type-artist")) return Tag.ARTIST
            if (type.contains("tag-type-copyright")) return Tag.COPYPRIGHT
            if (type.contains("tag-type-character")) return Tag.CHARACTER
            return Tag.UNKNOWN
        }

        fun String.startWithIgnoreSpace(start: String): Boolean {
            val a = toCharArray()
            for (i in a.indices) {
                if (a[i] != ' ')
                    return startsWith(start, startIndex = i)
            }
            return false
        }

        var nextLine = false
        val builder = StringBuilder()
        for (line in lines) {
            if (line.startWithIgnoreSpace("<ul id=\"tag-sideb")) {
                nextLine = true
                continue
            }
            if (nextLine)  //hier wird die Tag-Tabelle in einen String gesammelt
                if (line.startWithIgnoreSpace("</ul>"))
                    break
                else builder.append(line)
        }

        //Die Tags werden aus der Tabelle extrahiert
        val splitLines = builder.toString().split("</li>")
        for (l in splitLines) {
            try {
                val subStringType = l.substring(l.indexOf("<li class=\"") + 11)
                val type = getCurrentTagType(subStringType.substring(0, subStringType.indexOf("\"")))

                if (type != Tag.UNKNOWN) {
                    val nameSubstring = subStringType.substring(subStringType.indexOf("href=\"/post?") + 12)
                    val name = nameSubstring.substring(nameSubstring.indexOf(">") + 1, nameSubstring.indexOf("<")).replace(" ", "_")
                    tags += Tag(name, type)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return tags
    }
}