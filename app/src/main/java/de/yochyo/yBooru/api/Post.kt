package de.yochyo.yBooru.api

import org.json.JSONObject

interface Post {
    companion object {
        fun getPostFromJson(json: JSONObject): Post? {
            try {
                return object : Post {
                    override val id = json.getInt("id")
                    override val width = json.getInt("image_width")
                    override val height = json.getInt("image_height")
                    override val rating = json.getString("rating")
                    override val fileURL = json.getString("file_url")
                    override val fileLargeURL = json.getString("large_file_url")
                    override val filePreviewURL = json.getString("preview_file_url")
                    override val tagsGeneral = json.getString("tag_string_general").split(" ").map { Tag(it, Tag.GENERAL) }
                    override val tagsCharacter = json.getString("tag_string_character").split(" ").map { Tag(it, Tag.CHARACTER) }
                    override val tagsCopyright = json.getString("tag_string_copyright").split(" ").map { Tag(it, Tag.COPYPRIGHT) }
                    override val tagsArtist = json.getString("tag_string_artist").split(" ").map { Tag(it, Tag.ARTIST) }
                    override val tagsMeta = json.getString("tag_string_meta").split(" ").map { Tag(it, Tag.META) }
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
    val width: Int
    val height: Int
    val rating: String
    val fileURL: String
    val fileLargeURL: String
    val filePreviewURL: String

    val tagsGeneral: List<Tag>
    val tagsCharacter: List<Tag>
    val tagsCopyright: List<Tag>
    val tagsArtist: List<Tag>
    val tagsMeta: List<Tag>
}