package de.yochyo.booruapi.api

/**
 * @param id id of a post
 * @param extension Extension of the file (extension of sample or preview can be different)
 * @param width width of the image (width of sample or preview will be different most of the time)
 * @param height width of the image (width of sample or preview will be different most of the time)
 * @param rating string containing the rating, the string may differ depending on the api //TODO Make an Enum
 * @param fileSize fileSize in bytes (fileSize of sample or preview can be different)
 * @param fileURL Url of the biggest image
 * @param fileSampleURL either same as fileUrl or a compressed version
 * @param filePreviewURL Url to preview, preview is compressed to ~100x100 or less
 * @param tagString String containing all tags of a post
 */
open class Post(
    open val id: Int,
    open val extension: String,
    open val width: Int,
    open val height: Int,
    open val rating: String,
    open val fileSize: Int,
    open val fileURL: String,
    open val fileSampleURL: String,
    open val filePreviewURL: String,
    open val tagString: String,
) : Comparable<Post> {

    /**
     * Returns a list with all tags of a post. By default, all tags have default values.
     * This method should be overridden by Classes extending it.
     * @return
     */
    open fun getTags(): List<Tag> {
        return tagString.split(" ").map { Tag(it, TagType.UNKNOWN, 0) }.filter { it.name != "" }
    }

    override fun toString() = "[ID: $id] [${width}x$height]\n$fileURL\n$fileSampleURL\n$filePreviewURL\n{$tagString}"

    override fun compareTo(other: Post) = id.compareTo(other.id)
}