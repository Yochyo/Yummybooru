package de.yochyo.yummybooru.api

import de.yochyo.yummybooru.api.entities.Tag

abstract class Post {
    abstract val id: Int
    abstract val extension: String
    abstract val width: Int
    abstract val height: Int
    abstract val rating: String
    abstract val fileSize: Int
    abstract val fileURL: String
    abstract val fileSampleURL: String
    abstract val filePreviewURL: String

    abstract suspend fun getTags(): List<Tag>
    override fun toString() = "[$id] [${width}x$height]\n$fileURL\n$fileSampleURL\n$filePreviewURL"
}