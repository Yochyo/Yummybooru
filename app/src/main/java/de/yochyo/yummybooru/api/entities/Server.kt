package de.yochyo.yummybooru.api.entities

import android.content.Context
import androidx.room.*
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.utils.general.passwordToHash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Entity(tableName = "servers")
data class Server(var name: String, var api: String, var url: String, var userName: String = "", var password: String = "", var enableR18Filter: Boolean = false, @PrimaryKey val id: Int = -1) : Comparable<Server> {

    companion object {
        private var _currentServer: Server? = null
        val currentServer: Server
            get() {
                if (_currentServer == null || _currentServer!!.id != db.currentServerID) {
                    _currentServer = db.getServer(db.currentServerID) ?:
                            if(db.servers.isNotEmpty()) db.servers.first()
                            else Server("", "", "", "", "") //In case no server exist because of whatever bug may happened
                }
                return _currentServer!!
            }

        val currentID: Int get() = currentServer.id
    }

    @Ignore
    private var cachedPassword = password

    @Ignore
    var passwordHash: String = if(cachedPassword == "") "" else passwordToHash(password)
        get() {
            if (cachedPassword != password) {
                cachedPassword = password
                field = if(cachedPassword == "") "" else passwordToHash(password)
            }
            return field
        }
        private set

    @Ignore
    val urlHost: String = try {
        if(url == "") ""
        else URL(url).host
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }

    val isSelected: Boolean get() = currentServer.id == id

    suspend fun select(context: Context) {
        withContext(Dispatchers.Main) {
            if (currentServer != this@Server)
                SelectServerEvent.trigger(SelectServerEvent(context, currentServer, this@Server))
            db.currentServerID = id
            Api.selectApi(api, url)
            db.loadServerWithMutex(context)
            db.servers.notifyChange()
            updateMissingTypeTags(context)
            updateMissingTypeSubs(context)
        }
    }

    override fun compareTo(other: Server) = id.compareTo(other.id)
    override fun toString() = name
    override fun equals(other: Any?): Boolean {
        if (other is Server)
            return other.id == id
        return false
    }

    fun deleteServer(context: Context) {
        GlobalScope.launch { db.deleteServer(context, id) }
    }

    private fun updateMissingTypeTags(context: Context) {
        GlobalScope.launch {
            val current = currentServer
            val oldTags = db.tags.toCollection(ArrayList())
            val newTags = ArrayList<Tag>()
            for (tag in oldTags) { //Tags updaten
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
            val oldSubs = db.subs.toCollection(ArrayList())
            val newSubs = ArrayList<Subscription>()
            for (sub in oldSubs) { //Tags updaten
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