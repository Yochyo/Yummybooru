package de.yochyo.booruapi.api.danbooru

import com.fasterxml.jackson.annotation.JsonProperty
import de.yochyo.booruapi.api.Tag
import de.yochyo.booruapi.api.TagType
import java.util.Date

//TODO comments
data class DanbooruTag(
    val id: Int,
    override val name: String,
    @JsonProperty("category") val type: Int,
    @JsonProperty("post_count") override val count: Int,
    val createdAt: Date,
    val updatedAt: Date,
    val isLocked: Boolean,
) : Tag(name, typeToTypeEnum(type), count) {
    companion object {
        const val DANBOORU_GENERAL = 0
        const val DANBOORU_ARTIST = 1
        const val DANBOORU_COPYRIGHT = 3
        const val DANBOORU_CHARACTER = 4
        const val DANBOORU_META = 5
        const val DANBOORU_UNKNOWN = 99

        fun typeToTypeEnum(value: Int): TagType {
            return when (value) {
                DANBOORU_GENERAL -> TagType.GENERAL
                DANBOORU_ARTIST -> TagType.ARTIST
                DANBOORU_COPYRIGHT -> TagType.COPYRIGHT
                DANBOORU_CHARACTER -> TagType.CHARACTER
                DANBOORU_META -> TagType.META
                else -> TagType.UNKNOWN
            }
        }
    }
}