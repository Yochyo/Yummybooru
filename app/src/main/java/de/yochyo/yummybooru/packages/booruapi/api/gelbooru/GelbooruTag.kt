package de.yochyo.booruapi.api.gelbooru

import de.yochyo.booruapi.api.Tag
import de.yochyo.booruapi.api.TagType

//TODO comments
data class GelbooruTag(
    val id: Int,
    override val name: String,
    val type: Int,
    override val count: Int,
    val ambiguous: Boolean = false,
) : Tag(name, typeToTypeEnum(type), count) {
    companion object {
        const val GELBOORU_GENERAL = 0
        const val GELBOORU_ARTIST = 1
        const val GELBOORU_COPYRIGHT = 3
        const val GELBOORU_CHARACTER = 4
        const val GELBOORU_META = 5
        const val GELBOORU_UNKNOWN = 99

        fun typeToTypeEnum(value: Int): TagType {
            return when (value) {
                GELBOORU_GENERAL -> TagType.GENERAL
                GELBOORU_ARTIST -> TagType.ARTIST
                GELBOORU_COPYRIGHT -> TagType.COPYRIGHT
                GELBOORU_CHARACTER -> TagType.CHARACTER
                GELBOORU_META -> TagType.META
                else -> TagType.UNKNOWN
            }
        }
    }
}
