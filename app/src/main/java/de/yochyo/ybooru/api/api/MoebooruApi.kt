package de.yochyo.ybooru.api.api

import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.database.entities.Server
import de.yochyo.ybooru.database.entities.Tag
import org.json.JSONObject

class MoebooruApi(url: String) : Api(url) {
    override val name: String = "moebooru"
    override fun urlGetTag(name: String): String = "${url}tag.json?name=*$name" //TODO funktioniert das?
    override fun urlGetTags(beginSequence: String): String {
        return "${url}tag.json?name=$beginSequence*&limit=${Api.searchTagLimit}"
    }

    override fun urlGetPosts(page: Int, tags: Array<String>, limit: Int): String {
        return "${url}post.json?limit=$limit&page=$page&login=${Server.currentServer.userName}&password_hash=${Server.currentServer.passwordHash}"
    }

    override fun urlGetNewest(): String = "${url}post.json?limit=1&page=1"

    override fun getPostFromJson(json: JSONObject): Post? {
        try {
            val fileURL = json.getString("file_url")
            return object : Post {
                override val id = json.getInt("id")
                override val width = json.getInt("jpeg_width")
                override val height = json.getInt("jpeg_height")
                override val extension = fileURL.substring(fileURL.lastIndexOf(".") + 1)
                override val rating = json.getString("rating")
                override val fileSize = json.getInt("file_size")
                override val fileURL = fileURL
                override val fileLargeURL = json.getString("sample_url")
                override val filePreviewURL = json.getString("preview_url")
                override val tagsGeneral = json.getString("tags").split(" ").map { Tag(it, Tag.UNKNOWN) }.filter { it.name != "" }
                override val tagsCharacter = ArrayList<Tag>()
                override val tagsCopyright = ArrayList<Tag>()
                override val tagsArtist = ArrayList<Tag>()
                override val tagsMeta = ArrayList<Tag>()
                override fun toString(): String {
                    return "[$id] [${width}x$height]\nTags: $tagsGeneral\nTagsCharacters: $tagsCharacter\nTagsCopyright: $tagsCopyright\nTagsArtists: $tagsArtist\nTagsMeta: $tagsMeta\n$fileURL\n$fileLargeURL\n$filePreviewURL"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun getTagFromJson(json: JSONObject): Tag? {
        return try {
            var type = json.getInt("type")
            if (type !in 0..5)
                type = Tag.UNKNOWN
            Tag(json.getString("name"), type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}