package de.yochyo.booruapi.api

/**
 * @param name Name of a tag
 * @param tagType Type of a tag
 * @param count amount of posts that have this tag
 */
open class Tag(open val name: String, open val tagType: TagType, open val count: Int) : Comparable<Tag> {
    override fun toString(): String = name
    override fun compareTo(other: Tag) = name.compareTo(other.name)
}