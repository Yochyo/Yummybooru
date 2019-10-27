package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.documentFile
import org.json.JSONObject

object PreferencesBackup : BackupableEntity<String> {
    override fun toJSONObject(e: String, context: Context): JSONObject {
        val json = JSONObject()
        json.put("limit", db.limit)
        json.put("currentServerID", db.currentServerID)
        json.put("sortTags", db.sortTags)
        json.put("sortSubs", db.sortSubs)
        json.put("downloadOriginal", db.downloadOriginal)
        json.put("saveFile", db.saveFile.uri)
        return json
    }

    override fun toEntity(json: JSONObject, context: Context) {
        db.limit = json.getInt("limit")
        db.currentServerID = json.getInt("currentServerID")
        db.sortTags = json["sortTags"].toString()
        db.sortSubs = json["sortSubs"].toString()
        db.downloadOriginal = json.getBoolean("downloadOriginal")
        db.saveFile = documentFile(context, json["saveFile"].toString())
    }
}