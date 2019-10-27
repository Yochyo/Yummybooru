package de.yochyo.yummybooru.backup

import android.content.Context
import org.json.JSONObject

interface BackupableEntity<E> {
    fun toJSONObject(e: E, context: Context): JSONObject
    fun toEntity(json: JSONObject, context: Context)
}