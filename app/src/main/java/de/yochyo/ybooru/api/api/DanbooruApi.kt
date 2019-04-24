package de.yochyo.ybooru.api.api

import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.database.entities.Server
import de.yochyo.ybooru.database.entities.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DanbooruApi(url: String) : Api(url) {


    override val name = "danbooru"
    override fun urlGetTag(name: String): String = "${url}tags.json?search[name_matches]=$name"
    override fun urlGetTags(beginSequence: String): String {
        return "${url}tags.json?search[name_matches]=$beginSequence*&limit=${Api.searchTagLimit}"
    }

    override fun urlGetPosts(page: Int, tags: Array<String>, limit: Int): String {
        return "${url}posts.json?limit=$limit&page=$page&login=${Server.currentServer.userName}&password_hash=${Server.currentServer.passwordHash}"
    }

    override fun urlGetNewest(): String = "${url}posts.json?limit=1&page=1"

    override fun getPostFromJson(json: JSONObject): Post? {
        try {
            val tagsGeneral = json.getString("tag_string_general").split(" ").map { Tag(it, Tag.GENERAL) }.filter { it.name != "" }
            val tagsCharacter = json.getString("tag_string_character").split(" ").map { Tag(it, Tag.CHARACTER) }.filter { it.name != "" }
            val tagsCopyright = json.getString("tag_string_copyright").split(" ").map { Tag(it, Tag.COPYPRIGHT) }.filter { it.name != "" }
            val tagsArtist = json.getString("tag_string_artist").split(" ").map { Tag(it, Tag.ARTIST) }.filter { it.name != "" }
            val tagsMeta = json.getString("tag_string_meta").split(" ").map { Tag(it, Tag.META) }.filter { it.name != "" }
            val tags = ArrayList<Tag>(tagsGeneral.size + tagsCharacter.size + tagsCopyright.size + tagsArtist.size + tagsMeta.size)
            tags += tagsCopyright
            tags += tagsArtist
            tags += tagsCharacter
            tags += tagsGeneral
            tags += tagsMeta
            return object : Post() {
                override val id = json.getInt("id")
                override val width = json.getInt("image_width")
                override val height = json.getInt("image_height")
                override val extension = json.getString("file_ext")
                override val rating = json.getString("rating")
                override val fileSize = json.getInt("file_size")
                override val fileURL = json.getString("file_url")
                override val fileSampleURL = json.getString("large_file_url")
                override val filePreviewURL = json.getString("preview_file_url")
                override suspend fun getTags() = tags
                override fun toString(): String {
                    return "[$id] [${width}x$height]\nTags: $tags \n$fileURL\n$fileSampleURL\n$filePreviewURL"
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun getTagFromJson(json: JSONObject): Tag? {
            try {
                var type = json.getInt("category")
                if (type !in 0..5)
                    type = Tag.UNKNOWN
                return Tag(json.getString("name"), type, count = json.getInt("post_count"))
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
    }
}