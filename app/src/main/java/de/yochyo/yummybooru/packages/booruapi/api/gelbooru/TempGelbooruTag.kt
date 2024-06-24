package de.yochyo.booruapi.api.gelbooru

import com.fasterxml.jackson.annotation.JsonProperty
import de.yochyo.booruapi.api.TagType

data class TempGelbooruTag(
    val value: String,
    @JsonProperty("category") val typeString: String,
    val post_count: Int,
) {
    companion object {
        const val GELBOORU_GENERAL = "tag"
        const val GELBOORU_ARTIST = "artist"
        const val GELBOORU_COPYRIGHT = "copyright"
        const val GELBOORU_META = "metadata"
        const val GELBOORU_CHARACTER = "character"
        const val GELBOORU_UNKNOWN = "unknown"

        fun typeStringToEnum(type: String): TagType {
            return when (type) {
                GELBOORU_COPYRIGHT -> TagType.COPYRIGHT
                GELBOORU_META -> TagType.META
                GELBOORU_CHARACTER -> TagType.CHARACTER
                GELBOORU_GENERAL -> TagType.GENERAL
                GELBOORU_ARTIST -> TagType.ARTIST
                else -> TagType.UNKNOWN
            }
        }
    }
}
