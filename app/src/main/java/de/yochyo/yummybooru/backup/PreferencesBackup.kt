package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.documentFile
import org.json.JSONObject

object PreferencesBackup : BackupableEntity<String> {
    override fun toJSONObject(e: String, context: Context): JSONObject {
        val json = JSONObject()
        json.put("limit", context.db.limit)
        json.put("currentServerID", context.db.currentServerID)
        json.put("downloadOriginal", context.db.downloadOriginal)
        json.put("savePath", context.db.getSaveFolder(context).uri)
        return json
    }

    override suspend fun restoreEntity(json: JSONObject, context: Context) {
        context.db.limit = json.getInt("limit")
        context.db.currentServerID = json.getInt("currentServerID")
        context.db.downloadOriginal = json.getBoolean("downloadOriginal")
        context.db.setSaveFolder(documentFile(context, json["savePath"].toString()))
    }
}