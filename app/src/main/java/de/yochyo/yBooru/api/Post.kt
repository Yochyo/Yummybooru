package de.yochyo.yBooru.api

import org.json.JSONObject
import java.lang.Exception

interface Post {
    companion object {
        fun getPostFromJson(json: JSONObject): Post? {
            try {
                val post = object : Post {
                    override val id = json.getInt("id")
                    override val width = json.getInt("image_width")
                    override val height = json.getInt("image_height")
                    override val rating = json.getString("rating")
                    override val fileURL = json.getString("file_url")
                    override val fileLargeURL = json.getString("large_file_url")
                    override val filePreviewURL = json.getString("preview_file_url")
                    override val tagsGeneral = json.getString("tag_string_general").split(" ")
                    override val tagsCharacter = json.getString("tag_string_character").split(" ")
                    override val tagsCopyright = json.getString("tag_string_copyright").split(" ")
                    override val tagsArtist = json.getString("tag_string_artist").split(" ")
                    override val tagsMeta = json.getString("tag_string_meta").split(" ")
                    override fun toString(): String {
                        return "[$id] [${width}x$height]\nTags: $tagsGeneral\nTagsCharacters: $tagsCharacter\nTagsCopyright: $tagsCopyright\nTagsArtists: $tagsArtist\nTagsMeta: $tagsMeta\n$fileURL\n$fileLargeURL\n$filePreviewURL"
                    }
                }
                return post
            }catch(e: Exception){
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

    val tagsGeneral: List<String>
    val tagsCharacter: List<String>
    val tagsCopyright: List<String>
    val tagsArtist: List<String>
    val tagsMeta: List<String>
}