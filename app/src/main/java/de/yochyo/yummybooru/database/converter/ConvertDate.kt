package de.yochyo.yummybooru.database.converter

import androidx.room.TypeConverter
import java.util.*


object ConvertDate {
    @TypeConverter
    @JvmStatic
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    @TypeConverter
    @JvmStatic
    fun toTimestamp(creation: Date): Long {
        return creation.time
    }
}