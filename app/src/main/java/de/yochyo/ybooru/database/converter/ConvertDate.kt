package de.yochyo.ybooru.database.converter

import android.arch.persistence.room.TypeConverter
import java.text.DateFormat
import java.util.*


class DateConverter {
    @TypeConverter
    fun toDate(timestamp: String?): Date? {
        return if (timestamp == null) null
        else DateFormat.getInstance().parse(timestamp)
    }

    @TypeConverter
    fun toTimestamp(creation: Date?): String? {
        return if (creation != null) DateFormat.getInstance().format(creation)
        else null
    }
}