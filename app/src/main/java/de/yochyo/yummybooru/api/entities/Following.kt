package de.yochyo.yummybooru.api.entities

import androidx.room.ColumnInfo

class Following(@ColumnInfo(name = "last_id") val lastID: Int, @ColumnInfo(name = "last_count") val lastCount: Int)