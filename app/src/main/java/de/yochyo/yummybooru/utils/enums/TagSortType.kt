package de.yochyo.yummybooru.utils.enums

enum class TagSortType(val value: Int) {
    NAME_ASC(1), NAME_DES(2), DATE_ASC(3), DATE_DES(4);

    companion object {
        fun fromValue(value: Int): TagSortType {
            return when (value) {
                //TODO sync with sort_tag_comparator_values
                1 -> NAME_ASC
                2 -> NAME_DES
                3 -> DATE_ASC
                4 -> DATE_DES
                else -> throw Exception("value $value does not exist")
            }
        }
    }
}