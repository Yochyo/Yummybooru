package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.json.JSONObject
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.Logger
import java.util.*

object TagBackup : BackupableEntity<Tag> {
    override fun toJSONObject(e: Tag, context: Context): JSONObject {
        val json = JSONObject()
        json.put("name", e.name)
        json.put("type", e.type)
        json.put("isFavorite", e.isFavorite)
        json.put("creation", e.creation.time)
        json.put("serverID", e.serverID)
        json.put("lastID", e.following?.lastID ?: -1)
        json.put("lastCount", e.following?.lastCount ?: -1)
        return json
    }

    override suspend fun restoreEntity(json: JSONObject, context: Context) {
        try {
            var following: Following? = Following(json.getInt("lastID"), json.getInt("lastCount"))
            if (following?.lastID == -1 && following.lastCount == -1) following = null
            context.db.tagDao.insert(
                    Tag(json.getString("name"), json.getInt("type"), json.getBoolean("isFavorite"),
                            0, following, Date(json.getLong("creation")), json.getInt("serverID")))
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.log(e)
        }
    }

}