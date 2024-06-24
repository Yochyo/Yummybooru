package de.yochyo.booruapi.api.moebooru

import de.yochyo.booruapi.api.Tag
import de.yochyo.booruapi.api.TagType

data class MoebooruTag(
    val id: Int,
    override val name: String,
    val type: Int,
    override val count: Int,
    val ambiguous: Boolean,
) : Tag(name, typeToTypeEnum(type), count) {
    companion object {
        const val MOEBOORU_GENERAL = 0
        const val MOEBOORU_ARTIST = 1
        const val MOEBOORU_CIRCLE = 2
        const val MOEBOORU_COPYRIGHT = 3
        const val MOEBOORU_CHARACTER = 4
        const val MOEBOORU_META = 5
        const val MOEBOORU_UNKNOWN = 99

        fun typeToTypeEnum(value: Int): TagType {
            return when (value) {
                MOEBOORU_GENERAL -> TagType.GENERAL
                MOEBOORU_ARTIST -> TagType.ARTIST
                MOEBOORU_COPYRIGHT -> TagType.COPYRIGHT
                MOEBOORU_CHARACTER -> TagType.CHARACTER
                MOEBOORU_META -> TagType.META
                else -> TagType.UNKNOWN
            }
        }
    }
}