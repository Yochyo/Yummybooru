package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.documentFile
import org.json.JSONObject

object PreferencesBackup : BackupableEntity<String> {
    override fun toJSONObject(e: String, context: Context): JSONObject {
        val json = JSONObject()
        json.put("limit", db.limit)
        json.put("currentServerID", db.currentServerID)
        json.put("sortTags", db.sortTags)
        json.put("sortSubs", db.sortSubs)
        json.put("downloadOriginal", db.downloadOriginal)
        json.put("savePath", db.saveFolder.uri)
        return json
    }

    override suspend fun restoreEntity(json: JSONObject, context: Context) {
        db.limit = json.getInt("limit")
        db.currentServerID = json.getInt("currentServerID")
        db.sortTags = json["sortTags"].toString()
        db.sortSubs = json["sortSubs"].toString()
        db.downloadOriginal = json.getBoolean("downloadOriginal")
        db.saveFolder = documentFile(context, json["savePath"].toString())
    }
}