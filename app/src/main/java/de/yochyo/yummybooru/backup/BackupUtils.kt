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
            val serverArray = JSONArray()
            for (tag in context.db.tagDao.selectAll())
                tagArray.put(TagBackup.toJSONObject(tag, context))
            for (server in context.db.serverDao.selectAll())
                serverArray.put(ServerBackup.toJSONObject(server, context))
            json.put("tags", tagArray)
            json.put("servers", serverArray)
            json.put("preferences", PreferencesBackup.toJSONObject("", context))
            json.put("version", BuildConfig.VERSION_CODE)


            f.writeBytes(json.toString().toByteArray())
        }
    }

    suspend fun restoreBackup(byteArray: ByteArray, context: Context) {
        try {
            context.db.deleteEverything()
            var obj = updateRestoreObject(JSONObject(String(byteArray)))
            val tags = obj["tags"] as JSONArray
            val servers = obj["servers"] as JSONArray
            PreferencesBackup.restoreEntity(obj["preferences"] as JSONObject, context)
            for (i in 0 until servers.length())
                ServerBackup.restoreEntity(servers[i] as JSONObject, context)
            for (i in 0 until tags.length())
                TagBackup.restoreEntity(tags[i] as JSONObject, context)
        } catch (e: Exception) {
            Logger.log(e)
            e.printStackTrace()
        }
    }

    fun updateRestoreObject(json: JSONObject): JSONObject{
        val version = json["version"] as Int
        if(version < 8){
            (json["preferences"] as JSONObject).put("downloadWebm", true)
            val subs = json["subs"] as JSONArray
            val tags = json["tags"] as JSONArray
            val newTags = JSONArray()
            for (i in 0 until tags.length()) { //add sub infos to tag
                val tag = tags[i] as JSONObject
                val tagName = tag["name"].toString()
                tag.remove("count")
                for (s in 0 until subs.length()) { //check if a sub exists
                    val sub = subs[s] as JSONObject
                    val subName = sub["name"].toString()
                    if (tagName == subName) {
                        tag.put("lastID", sub["lastID"])
                        tag.put("lastCount", sub["lastCount"])
                        subs.remove(s)
                        break
                    }
                }
                newTags.put(tag)
            }
            for(i in 0 until subs.length()){ //add all subs that didn't have a tag
                val sub = subs[i] as JSONObject
                val subName = sub["name"].toString()
                var exists = false
                for(t in 0 until tags.length()){
                    val tag = tags[t] as JSONObject
                    val tagName = tag["name"].toString()
                    if(tagName == subName){
                        exists = true
                        break
                    }
                }
                if(!exists) {
                    newTags.put(sub)
                }
            }


            json.remove("tags")
            json.remove("subs")
            json.put("tags", newTags)
        }
        return json
    }

    private fun createBackupFile(): File {
        val file = File("$directory/backup" + System.currentTimeMillis() + ".yBooru")
        file.createNewFile()
        return file
    }
}