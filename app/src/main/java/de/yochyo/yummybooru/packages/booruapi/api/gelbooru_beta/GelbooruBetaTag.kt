package de.yochyo.booruapi.api.gelbooru_beta

import de.yochyo.booruapi.api.Tag
import de.yochyo.booruapi.api.TagType

data class GelbooruBetaTag(
    val id: Int,
    override val name: String,
    val type: Int,
    override val count: Int,
    val ambiguous: Boolean,
) : Tag(name, typeStringToEnum(type), count) {
    companion object {
        const val GELBOORU_BETA_GENERAL = 0
        const val GELBOORU_BETA_ARTIST = 1
        const val GELBOORU_BETA_COPYRIGHT = 3
        const val GELBOORU_BETA_META = 5
        const val GELBOORU_BETA_CHARACTER = 4
        const val GELBOORU_BETA_UNKNOWN = 99

        fun typeStringToEnum(type: Int): TagType {
            return when (type) {
                GELBOORU_BETA_COPYRIGHT -> TagType.COPYRIGHT
                GELBOORU_BETA_META -> TagType.META
                GELBOORU_BETA_CHARACTER -> TagType.CHARACTER
                GELBOORU_BETA_GENERAL -> TagType.GENERAL
                GELBOORU_BETA_ARTIST -> TagType.ARTIST
                else -> TagType.UNKNOWN
            }
        }
    }
}