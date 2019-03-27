package de.yochyo.ybooru.database.converter

import android.arch.persistence.room.TypeConverter
import java.util.*


class DateConverter {
    @TypeConverter
    fun toDate(timestamp: Long?): Date? {
        return if (timestamp == null) null
        else Date(timestamp)
    }

    @TypeConverter
    fun toTimestamp(creation: Date?): Long? {
        return creation?.time
    }
}