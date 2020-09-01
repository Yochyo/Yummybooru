package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.json.JSONArray
import de.yochyo.json.JSONObject
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.configPath
import de.yochyo.yummybooru.utils.general.log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object BackupUtils {
    val directory = "$configPath/backup"

    val dir = File(directory)

    init {
        dir.mkdirs()
    }

    suspend fun createBackup(context: Context) {
        withContext(Dispatchers.IO) {
            val json = JSONObject()
            val tagArray = JSONArray()
            val serverArray = JSONArray()
            for (tag in context.db.tagDao.selectAll())
                tagArray.put(TagBackup.toJSONObject(tag, context))
            for (server in context.db.serverDao.selectAll())
                serverArray.put(ServerBackup.toJSONObject(server, context))
            json.put("tags", tagArray)
            json.put("servers", serverArray)
            json.put("preferences", PreferencesBackup.toJSONObject("", context))
            json.put("version", BuildConfig.VERSION_CODE)

            val f = createBackupFile()
            f.writeBytes(json.toString().toByteArray())
        }
    }

    suspend fun restoreBackup(byteArray: ByteArray, context: Context) {
        try {
            val obj = updateRestoreObject(JSONObject(String(byteArray)))
            val tags = obj.getJSONArray("tags")
            val servers = obj.getJSONArray("servers")
            withContext(Dispatchers.IO) {
                context.db.deleteEverything()
                PreferencesBackup.restoreEntity(obj.getJSONObject("preferences"), context)
                servers.map { ServerBackup.restoreEntity(it as JSONObject, context) }
                tags.map { launch { TagBackup.restoreEntity(it as JSONObject, context) } }.joinAll()
                context.db.clearCache()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            e.log()
        }
    }

    fun updateRestoreObject(json: JSONObject): JSONObject {
        try {
            val version = json["version"] as Int
            if (version < 9) {
                upgradeToVersion9(json)
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            e.log()
        }
        return json
    }

    private fun upgradeToVersion9(json: JSONObject): JSONObject {
        try {
            val version = json["version"] as Int
            if (version < 9) {
                json.getJSONObject("preferences").put("downloadWebm", true)
                val following = json.getJSONArray("subs")
                val tags = json.getJSONArray("tags")
                for (tag in tags) {
                    val tag = tag as JSONObject
                    val name = tag.getString("name")
                    val serverID = tag.getInt("serverID")
                    val follow = following.find { (it as JSONObject).getString("name") == name && it.getInt("serverID") == serverID } as JSONObject?
                    tag.put("lastID", follow?.getInt("lastID") ?: -1)
                    tag.put("lastCount", follow?.getInt("lastCount") ?: -1)
                }
                for (follow in following) {
                    val follow = follow as JSONObject
                    val name = follow.getString("name")
                    val serverID = follow.getInt("serverID")
                    val tag = tags.find { (it as JSONObject).getString("name") == name && it.getInt("serverID") == serverID } as JSONObject?
                    if (tag == null) {
                        tags.put(follow)
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            e.log()
        }
        return json
    }

    private fun createBackupFile(): File {
        val file = File("$directory/backup" + System.currentTimeMillis() + ".yBooru")
        file.createNewFile()
        return file
    }
}