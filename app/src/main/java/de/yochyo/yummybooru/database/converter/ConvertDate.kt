package de.yochyo.yummybooru.database.converter

import java.util.*


object ConvertDate {
    fun toDate(timestamp: Long): Date {
        return Date(timestamp)
    }

    fun toTimestamp(creation: Date): Long {
        return creation.time
    }
}