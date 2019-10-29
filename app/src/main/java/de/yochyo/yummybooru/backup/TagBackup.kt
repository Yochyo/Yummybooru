package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.Logger
import org.json.JSONObject
import java.lang.Exception
import java.util.*

object TagBackup : BackupableEntity<Tag> {
    override fun toJSONObject(e: Tag, context: Context): JSONObject {
        val json = JSONObject()
        json.put("name", e.name)
        json.put("type", e.type)
        json.put("isFavorite", e.isFavorite)
        json.put("creation", e.creation.time)
        json.put("serverID", e.serverID)
        json.put("count", e.count)
        return json
    }

    override suspend fun restoreEntity(json: JSONObject, context: Context) {
        try{
            db.tagDao.insert(
                    Tag(json.getString("name"), json.getInt("type"), json.getBoolean("isFavorite"),
                            Date(json.getLong("creation")), json.getInt("serverID"), json.getInt("count")))
        }catch(e: Exception){
            e.printStackTrace()
            Logger.log(e)
        }
    }

}