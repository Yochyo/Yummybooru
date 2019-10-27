package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.parseURL
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONObject

object ServerBackup : BackupableEntity<Server> {
    override fun toJSONObject(e: Server, context: Context): JSONObject {
        val json = JSONObject()
        json.put("name", e.name)
        json.put("api", e.api)
        json.put("urlHost", e.urlHost)
        json.put("userName", e.userName)
        json.put("password", e.password)
        json.put("enableR18Filter", e.enableR18Filter)
        return json
    }

    override fun toEntity(json: JSONObject, context: Context) {
        val server = Server(json.getString("name"), json.getString("api"),
                json.getString("urlHost"), json.getString("userName"), json.getString("password"),
                json.getBoolean("enableR18Filter"))
        GlobalScope.launch { db.addServer(context, server) }
    }

}