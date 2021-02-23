package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.booruapi.api.TagType
import de.yochyo.json.JSONObject
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.utils.general.sendFirebase
import java.util.*

object TagBackup : BackupableEntity<Tag> {
    override fun toJSONObject(e: Tag, context: Context): JSONObject {
        val json = JSONObject()
        json.put("name", e.name)
        json.put("type", e.type.value)
        json.put("isFavorite", e.isFavorite)
        json.put("creation", e.creation.time)
        json.put("serverID", e.serverId)
        json.put("lastID", e.following?.lastID ?: -1)
        json.put("lastCount", e.following?.lastCount ?: -1)
        return json
    }

    override suspend fun restoreEntity(json: JSONObject, context: Context) {
        TODO()
    }

    fun restoreEntity2(json: JSONObject, context: Context): Tag? {
        return try {
            val lastId = json.getInt("lastID")
            val lastCount = json.getInt("lastCount")
            val following = if (lastId == -1 || lastCount == -1) null else Following(lastId, lastCount)
            Tag(
                json.getString("name"), TagType.valueOf(json.getInt("type")), json.getBoolean("isFavorite"),
                0, following, Date(json.getLong("creation")), json.getInt("serverID")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            e.sendFirebase()
            null
        }
    }

}