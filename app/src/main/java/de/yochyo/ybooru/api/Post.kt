package de.yochyo.ybooru.api

import android.content.Context
import org.json.JSONObject

interface Post {
    companion object {
        fun getPostFromJson(context: Context, json: JSONObject): Post? {
            try {
                return object : Post {
                    override val id = json.getInt("id")
                    override val width = json.getInt("image_width")
                    override val height = json.getInt("image_height")
                    override val extension = json.getString("file_ext")
                    override val rating = json.getString("rating")
                    override val fileSize = json.getInt("file_size")
                    override val fileURL = json.getString("file_url")
                    override val fileLargeURL = json.getString("large_file_url")
                    override val filePreviewURL = json.getString("preview_file_url")
                    override val tagsGeneral = json.getString("tag_string_general").split(" ").map { Tag(context, it, Tag.GENERAL) }.filter { it.name != "" }
                    override val tagsCharacter = json.getString("tag_string_character").split(" ").map { Tag(context, it, Tag.CHARACTER) }.filter { it.name != "" }
                    override val tagsCopyright = json.getString("tag_string_copyright").split(" ").map { Tag(context, it, Tag.COPYPRIGHT) }.filter { it.name != "" }
                    override val tagsArtist = json.getString("tag_string_artist").split(" ").map { Tag(context, it, Tag.ARTIST) }.filter { it.name != "" }
                    override val tagsMeta = json.getString("tag_string_meta").split(" ").map { Tag(context, it, Tag.META) }.filter { it.name != "" }
                    override fun toString(): String {
                        return "[$id] [${width}x$height]\nTags: $tagsGeneral\nTagsCharacters: $tagsCharacter\nTagsCopyright: $tagsCopyright\nTagsArtists: $tagsArtist\nTagsMeta: $tagsMeta\n$fileURL\n$fileLargeURL\n$filePreviewURL"
                    }
                }
            } catch (e: Exception) {
                return null
            }
        }
    }

    val id: Int
    val extension: String
    val width: Int
    val height: Int
    val rating: String
    val fileSize: Int
    val fileURL: String
    val fileLargeURL: String
    val filePreviewURL: String

    val tagsGeneral: List<Tag>
    val tagsCharacter: List<Tag>
    val tagsCopyright: List<Tag>
    val tagsArtist: List<Tag>
    val tagsMeta: List<Tag>
}