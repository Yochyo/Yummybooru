package de.yochyo.yummybooru.backup

import android.content.Context

interface BackupableEntity<E> {
    fun toString(e: E, context: Context): String
    fun toEntity(s: String, context: Context)

}