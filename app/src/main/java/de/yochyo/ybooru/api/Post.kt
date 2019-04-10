package de.yochyo.ybooru.api

import de.yochyo.ybooru.database.entities.Tag

interface Post {
    val id: Int
    val extension: String
    val width: Int
    val height: Int
    val rating: String
    val fileSize: Int
    val fileURL: String
    val fileSampleURL: String
    val filePreviewURL: String

    val tagsGeneral: List<Tag>
    val tagsCharacter: List<Tag>
    val tagsCopyright: List<Tag>
    val tagsArtist: List<Tag>
    val tagsMeta: List<Tag>
}