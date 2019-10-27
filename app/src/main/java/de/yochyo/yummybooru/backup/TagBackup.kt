package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject
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

    override fun toEntity(json: JSONObject, context: Context) {
        GlobalScope.launch {
            db.tagDao.insert(
                    Tag(json.getString("name"), json.getInt("type"), json.getBoolean("isFavorite"),
                            Date(json.getLong("creation")), json.getInt("serverID"), json.getInt("count")))
        }
    }

}