package de.yochyo.yummybooru.api.entities

import android.arch.persistence.room.*
import android.content.Context
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.utils.Logger
import de.yochyo.yummybooru.utils.passwordToHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Entity(tableName = "servers")
data class Server(var name: String, var api: String, var url: String, var userName: String = "", var password: String = "", var enableR18Filter: Boolean = false, @PrimaryKey val id: Int = -1) : Comparable<Server> {
    companion object {
        var currentServer: Server = db.getServer(db.currentServerID)!!
            get() {
                if (currentServer.id != db.currentServerID) currentServer = db.getServer(db.currentServerID)!!
                return field
            }
            private set
        val currentID: Int get() = currentServer.id
    }

    @Ignore
    private var cachedPassword = password

    @Ignore
    var passwordHash: String = passwordToHash(password)
        get() {
            if (cachedPassword != password) {
                cachedPassword = password
                field = passwordToHash(password)
            }
            return field
        }
        private set

    val urlHost: String = try {
        URL(url).host!!
    } catch (e: Exception) {
        Logger.log(e)
        e.printStackTrace()
        ""
    }

    val isSelected: Boolean get() = currentServer.id == id

    suspend fun select(context: Context) {
        withContext(Dispatchers.Main) {
            SelectServerEvent.trigger(SelectServerEvent(context, currentServer, this@Server))
            db.currentServerID = id
            currentServer = this@Server
            Api.selectApi(api, url)
            db.loadServerWithMutex(context)
            db.servers.notifyChange()
            currentServer.updateMissingTypeTags(context)
            currentServer.updateMissingTypeSubs(context)
        }
    }

    fun deleteServer(context: Context) {
        GlobalScope.launch { db.deleteServer(context, id) }
    }

    override fun compareTo(other: Server) = id.compareTo(other.id)
    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (other is Server)
            return other.id == id
        return false
    }

    private fun updateMissingTypeTags(context: Context) {
        GlobalScope.launch {
            val current = currentServer
            val newTags = ArrayList<Tag>()
            for (tag in db.tags) { //Tags updaten
                if (tag.type == Tag.UNKNOWN) {
                    val t = Api.getTag(tag.name)
                    newTags += t.copy(isFavorite = tag.isFavorite, creation = tag.creation, serverID = tag.serverID)
                }
            }
            for (tag in newTags) { //Tags ersetzen
                if (currentServer == current) {
                    if (tag.type != Tag.UNKNOWN) {
                        db.deleteTag(context, tag.name)
                        db.addTag(context, tag)
                    }
                } else break
            }
        }
    }

    private fun updateMissingTypeSubs(context: Context) {
        GlobalScope.launch {
            val current = currentServer
            val newSubs = ArrayList<Subscription>()
            for (sub in db.subs) { //Tags updaten
                if (sub.type == Tag.UNKNOWN) {
                    val s = Subscription.fromTag(Api.getTag(sub.name))
                    newSubs += s.copy(isFavorite = sub.isFavorite, creation = sub.creation, serverID = sub.serverID)
                }
            }
            for (sub in newSubs) { //Tags ersetzen
                if (current == currentServer) {
                    if (sub.type != Tag.UNKNOWN) {
                        db.deleteSubscription(context, sub.name)
                        db.addSubscription(context, sub)
                    }
                } else break
            }
        }
    }
}

@Dao
interface ServerDao {
    @Insert
    fun insert(server: Server)

    @Delete
    fun delete(server: Server)

    @Update
    fun update(server: Server)

    @Query("SELECT * FROM servers")
    fun getAllServers(): List<Server>
}