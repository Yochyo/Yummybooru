package de.yochyo.yummybooru.database.converter

import androidx.room.TypeConverter
import de.yochyo.booruapi.api.TagType

object ConvertTagType {
    @TypeConverter
    @JvmStatic
    fun toTagType(value: Int) = TagType.valueOf(value)

    @TypeConverter
    @JvmStatic
    fun toInteger(type: TagType) = type.value
}