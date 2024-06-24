package de.yochyo.booruapi.api.my_imouto

import com.fasterxml.jackson.annotation.JsonProperty
import de.yochyo.booruapi.api.Tag
import de.yochyo.booruapi.api.TagType
import java.util.Date

data class MyImoutoTag(
    val id: Int,
    override val name: String,
    @JsonProperty("tag_type") val type: Int,
    @JsonProperty("post_count") override val count: Int,
    val cachedRelated: String,
    val cachedRelatedExpiresOn: Date,
    val isAmbiguous: Boolean,
) : Tag(name, typeToTypeEnum(type), count) {
    companion object {
        const val MY_IMOUTO_GENERAL = 0
        const val MY_IMOUTO_ARTIST = 1
        const val MY_IMOUTO_CIRCLE = 2
        const val MY_IMOUTO_COPYRIGHT = 3
        const val MY_IMOUTO_CHARACTER = 4
        const val MY_IMOUTO_META = 5
        const val MY_IMOUTO_UNKNOWN = 99

        fun typeToTypeEnum(value: Int): TagType {
            return when (value) {
                MY_IMOUTO_GENERAL -> TagType.GENERAL
                MY_IMOUTO_ARTIST -> TagType.ARTIST
                MY_IMOUTO_COPYRIGHT -> TagType.COPYRIGHT
                MY_IMOUTO_CHARACTER -> TagType.CHARACTER
                MY_IMOUTO_META -> TagType.META
                else -> TagType.UNKNOWN
            }
        }
    }
}