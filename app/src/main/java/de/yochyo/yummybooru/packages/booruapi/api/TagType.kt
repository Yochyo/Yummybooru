package de.yochyo.booruapi.api

enum class TagType(val value: Int) {
    GENERAL(0), ARTIST(1), COPYRIGHT(3), CHARACTER(4), META(5), UNKNOWN(99);

    companion object {
        fun valueOf(value: Int): TagType {
            return when (value) {
                0 -> GENERAL
                1 -> ARTIST
                3 -> COPYRIGHT
                4 -> CHARACTER
                5 -> META
                else -> UNKNOWN
            }
        }
    }

}