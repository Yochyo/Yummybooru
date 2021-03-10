package de.yochyo.yummybooru.database.converter

import androidx.room.TypeConverter

object ConvertBoolean {
    @TypeConverter
    @JvmStatic
    fun toBoolean(int: Int) = int == 1

    @TypeConverter
    @JvmStatic
    fun toInteger(boolean: Boolean) = if (boolean) 1 else 0
}