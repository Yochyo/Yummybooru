package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.documentFile
import de.yochyo.json.JSONObject

object PreferencesBackup : BackupableEntity<String> {
    override fun toJSONObject(e: String, context: Context): JSONObject {
        val json = JSONObject()
        json.put("limit", context.db.limit)
        json.put("currentServerID", context.db.currentServerID)
        json.put("downloadOriginal", context.db.downloadOriginal)
        json.put("downloadWebm", context.db.downloadWebm)
        json.put("savePath", context.db.saveFolder.uri)
        return json
    }

    override suspend fun restoreEntity(json: JSONObject, context: Context) {
        context.db.limit = json.getInt("limit")
        context.db.currentServerID = json.getInt("currentServerID")
        context.db.downloadOriginal = json.getBoolean("downloadOriginal")
        context.db.downloadWebm = json.getBoolean("downloadWebm")
        context.db.saveFolder = documentFile(context, json["savePath"].toString())
    }
}