package de.yochyo.yummybooru.backup

import android.content.Context
import org.json.JSONObject

interface BackupableEntity<E> {
    fun toJSONObject(e: E, context: Context): JSONObject
    suspend fun restoreEntity(json: JSONObject, context: Context)
}