package de.yochyo.yummybooru.backup

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventmanager.EventHandler
import de.yochyo.json.JSONArray
import de.yochyo.json.JSONObject
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.general.sendFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object BackupUtils {
    suspend fun createBackup(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            val json = JSONObject()
            val tagArray = JSONArray()
            val serverArray = JSONArray()
            for (tag in context.db.tagDao.selectAllNoFlow())
                tagArray.put(TagBackup.toJSONObject(tag, context))
            for (server in context.db.serverDao.selectAllNoFlow())
                serverArray.put(ServerBackup.toJSONObject(server, context))
            json.put("tags", tagArray)
            json.put("servers", serverArray)
            json.put("preferences", PreferencesBackup.toJSONObject("", context))
            json.put("version", BuildConfig.VERSION_CODE)

            val f = createBackupFile(context)
            if (f != null) {
                FileUtils.writeBytes(context, f, json.toString().toByteArray().inputStream())
                true
            } else false
        }
    }

    suspend fun restoreBackup(byteArray: ByteArray, context: Context, observable: EventHandler<OnChangeObjectEvent<Int, Int>>) {
        withContext(Dispatchers.IO) {
            try {
                val obj = updateRestoreObject(JSONObject(String(byteArray)))

                val prefs = obj.getJSONObject("preferences")
                val servers = obj.getJSONArray("servers").mapNotNull { ServerBackup.restoreEntity2(it as JSONObject, context) }
                val tags = obj.getJSONArray("tags").mapNotNull { TagBackup.restoreEntity2(it as JSONObject, context) }
                val size = 1 + tags.size + servers.size

                var progress = 0
                suspend fun incrementProgress(add: Int) = withContext(Dispatchers.Main) {
                    progress += add
                    observable.trigger(OnChangeObjectEvent(progress, size))
                }


                val prefsInt = prefs.getJSONObject("integers")
                val strCurServer = context.getString(R.string.currentServer)
                var updatedSelectedServer = false

                incrementProgress(0)
                context.db.deleteEverything()

                for (server in servers) {
                    val tags = tags.filter { it.serverId == server.id }
                    val newId = context.db.addServer(server).toInt()
                    context.db.addTags(tags.map { it.copy(serverId = newId) })
                    incrementProgress(1 + tags.size)
                    if (!updatedSelectedServer && prefsInt.has(strCurServer) && prefsInt.getInt(strCurServer) == server.id) {
                        updatedSelectedServer = true
                        prefsInt.put(strCurServer, newId)
                    }
                }

                PreferencesBackup.restoreEntity(prefs, context)
                observable.trigger(OnChangeObjectEvent(size, size))

            } catch (e: Exception) {
                e.printStackTrace()
                e.sendFirebase()
            }
        }
    }

    fun updateRestoreObject(json: JSONObject): JSONObject {
        try {
            val version = json["version"] as Int
            if (version < 9)
                upgradeToVersion9(json)
            if (version < 10)
                upgradeToVersion10(json)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            e.sendFirebase()
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
            e.sendFirebase()
        }
        return json
    }

    private fun upgradeToVersion10(json: JSONObject): JSONObject {
        try {
            val preferences = json.getJSONObject("preferences")
            val version = json["version"] as Int
            if (version < 10) {
                val strings = JSONObject()
                val integers = JSONObject()
                val longs = JSONObject()
                val booleans = JSONObject()
                val floats = JSONObject()
                preferences.put("strings", strings)
                preferences.put("integers", integers)
                preferences.put("longs", longs)
                preferences.put("booleans", booleans)
                preferences.put("floats", floats)

                integers.put("limit", preferences.getInt("limit"))
                integers.put("currentServerID", preferences.getInt("currentServerID"))
                booleans.put("downloadOriginal", preferences.getBoolean("downloadOriginal"))
                booleans.put("downloadWebm", preferences.getBoolean("downloadWebm"))
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            e.sendFirebase()
        }
        return json
    }

    private suspend fun createBackupFile(context: Context): DocumentFile? {
        return FileUtils.createFileOrNull(context, "backup", "${Date().time}.yBooru", "ybooru")
    }
}