package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.Logger
import de.yochyo.yummybooru.utils.general.configPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object BackupUtils {
    //TODO ObjectSerializer benutzen, Eine Preferences Klasse schreiben
    val directory = "$configPath/backup"

    val dir = File(directory)

    init {
        dir.mkdirs()
    }

    fun createBackup(context: Context) {
        GlobalScope.launch(Dispatchers.IO) {
            val f = createBackupFile()
            val json = JSONObject()
            val tagArray = JSONArray()
            val subArray = JSONArray()
            val serverArray = JSONArray()
            for (tag in db.tagDao.selectAll())
                tagArray.put(TagBackup.toJSONObject(tag, context))
            for (sub in db.subDao.selectAll())
                subArray.put(SubscriptionBackup.toJSONObject(sub, context))
            for (server in db.serverDao.selectAll())
                serverArray.put(ServerBackup.toJSONObject(server, context))
            json.put("tags", tagArray)
            json.put("subs", subArray)
            json.put("servers", serverArray)
            json.put("preferences", PreferencesBackup.toJSONObject("", context))
            json.put("version", BuildConfig.VERSION_CODE)


            f.writeBytes(json.toString().toByteArray())
        }
    }

    suspend fun restoreBackup(byteArray: ByteArray, context: Context) {
        try {
            db.deleteEverything()
            val obj = JSONObject(String(byteArray))
            val tags = obj["tags"] as JSONArray
            val subs = obj["subs"] as JSONArray
            val servers = obj["servers"] as JSONArray
            PreferencesBackup.restoreEntity(obj["preferences"] as JSONObject, context)
            for (i in 0 until servers.length())
                ServerBackup.restoreEntity(servers[i] as JSONObject, context)
            for (i in 0 until tags.length())
                TagBackup.restoreEntity(tags[i] as JSONObject, context)
            for (i in 0 until subs.length())
                SubscriptionBackup.restoreEntity(subs[i] as JSONObject, context)
        } catch (e: Exception) {
            Logger.log(e)
            e.printStackTrace()
        }
    }

    private fun createBackupFile(): File {
        val file = File("$directory/backup" + System.currentTimeMillis() + ".yBooru")
        file.createNewFile()
        return file
    }
}