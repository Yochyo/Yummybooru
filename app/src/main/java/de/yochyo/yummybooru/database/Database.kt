package de.yochyo.yummybooru.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.documentfile.provider.DocumentFile
import de.yochyo.eventcollection.EventCollection
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.dao.ServerDao
import de.yochyo.yummybooru.database.dao.SubDao
import de.yochyo.yummybooru.database.dao.TagDao
import de.yochyo.yummybooru.database.utils.Upgrade
import de.yochyo.yummybooru.events.events.*
import de.yochyo.yummybooru.utils.general.createDefaultSavePath
import de.yochyo.yummybooru.utils.general.documentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import org.jetbrains.anko.db.dropTable
import java.util.*


class Database(context: Context) : ManagedSQLiteOpenHelper(context, "db", null, 2) {
    private val lock = Mutex()
    private val prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)

    companion object {
        private var instance: Database? = null
        private val dbLock = Any()
        fun getDatabase(context: Context): Database {
            synchronized(dbLock) {
                if (instance == null) {
                    instance = Database(context)
                    instance!!.servers.onUpdate.registerListener { GlobalScope.launch(Dispatchers.Main) { UpdateServersEvent.trigger(UpdateServersEvent(context, instance!!.servers)) } }
                    instance!!.tags.onUpdate.registerListener { GlobalScope.launch(Dispatchers.Main) { UpdateTagsEvent.trigger(UpdateTagsEvent(context, instance!!.tags)) } }
                    instance!!.subs.onUpdate.registerListener { GlobalScope.launch(Dispatchers.Main) { UpdateSubsEvent.trigger(UpdateSubsEvent(context, instance!!.subs)) } }
                    GlobalScope.launch {
                        instance!!.loadServers(context)
                    }
                }
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        tagDao.createTable()
        subDao.createTable()
        serverDao.createTable()

        db.rawQuery("INSERT INTO servers (name,api,url,userName,password,enableR18Filter) VALUES ('Danbooru', 'danbooru', 'https://danbooru.donmai.us/', '', '', 0);", emptyArray())
        db.rawQuery("INSERT INTO servers (name,api,url,userName,password,enableR18Filter) VALUES ('Konachan', 'moebooru', 'https://konachan.com/', '', '', 0);", emptyArray())
        db.rawQuery("INSERT INTO servers (name,api,url,userName,password,enableR18Filter) VALUES ('Yande.re', 'moebooru', 'https://yande.re/', '', '', 0);", emptyArray())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Upgrade.upgradeFromTo(db, oldVersion, newVersion)
    }

    val servers: EventCollection<Server> = EventCollection(TreeSet())
    val tags: EventCollection<Tag> = EventCollection(TreeSet())
    val subs: EventCollection<Subscription> = EventCollection(TreeSet())
    suspend fun loadServers(context: Context) {
        withContext(Dispatchers.Default) {
            val se: List<Server> = serverDao.selectAll()
            servers.clear()
            servers.addAll(se)
            Server.getCurrentServer(context).select(context)
        }
    }

    suspend fun loadServerWithMutex(context: Context) = lock.withLock { loadServer(context) }
    suspend fun loadServer(context: Context) {
        withContext(Dispatchers.Default) {
            val server = Server.getCurrentServer(context)
            loadTags(context, server.id)
            loadSubscriptions(context, server.id)
        }
    }

    suspend fun loadTagsWithMutex(context: Context, serverID: Int) = lock.withLock { loadTags(context, serverID) }
    suspend fun loadTags(context: Context, serverID: Int) {
        withContext(Dispatchers.Default) {
            val t = tagDao.selectWhereID(serverID)
            withContext(Dispatchers.Main) {
                tags.clear()
                tags.addAll(t)
            }
        }
    }

    suspend fun loadSubscriptionsWithMutex(context: Context, serverID: Int) = lock.withLock { loadSubscriptions(context, serverID) }
    suspend fun loadSubscriptions(context: Context, serverID: Int) {
        withContext(Dispatchers.Default) {
            val s = subDao.selectWhereID(serverID)
            subs.clear()
            subs.addAll(s)
        }
    }

    fun getServer(id: Int) = servers.find { it.id == id }

    suspend fun addServerWithMutex(context: Context, server: Server) = lock.withLock { addServer(context, server) }
    suspend fun addServer(context: Context, server: Server) {
        withContext(Dispatchers.Default) {
            val s = getServer(server.id)
            if (s == null) {
                val serverCopy = server.copy(id = nextServerID)
                servers.add(serverCopy)
                serverDao.insert(serverCopy)
                withContext(Dispatchers.Main) { AddServerEvent.trigger(AddServerEvent(context, serverCopy)) }
            }
        }
    }

    suspend fun deleteServerWithMutex(context: Context, id: Int) = lock.withLock { deleteServer(context, id) }
    suspend fun deleteServer(context: Context, id: Int) {
        withContext(Dispatchers.Default) {
            val s = servers.find { id == it.id }
            if (s != null && !s.isSelected(context)) {
                servers.remove(s)
                serverDao.delete(s)
                for (tag in tagDao.selectWhereID(s.id)) tagDao.delete(tag)
                for (sub in subDao.selectWhereID(s.id)) subDao.delete(sub)
                withContext(Dispatchers.Main) { DeleteServerEvent.trigger(DeleteServerEvent(context, s)) }
            }
        }
    }

    suspend fun changeServerWithMutex(context: Context, changedServer: Server) = lock.withLock { changeServer(context, changedServer) }
    suspend fun changeServer(context: Context, changedServer: Server) {
        withContext(Dispatchers.Default) {
            val s = servers.find { it.id == changedServer.id }
            if (s != null) {
                val wasCurrentServer = Server.getCurrentServer(context) == changedServer
                servers.remove(s)
                servers.add(changedServer)
                if (wasCurrentServer) changedServer.select(context)
                serverDao.update(changedServer)
                withContext(Dispatchers.Main) { ChangeServerEvent.trigger(ChangeServerEvent(context, changedServer, s)) }
            }
        }
    }

    fun getTag(name: String) = tags.find { it.name == name }

    suspend fun addTagWithMutex(context: Context, tag: Tag) = lock.withLock { addTag(context, tag) }
    suspend fun addTag(context: Context, tag: Tag): Tag {
        return withContext(Dispatchers.Default) {
            val t = getTag(tag.name)
            if (t == null) {
                tags.add(tag)
                tagDao.insert(tag)
                withContext(Dispatchers.Main) { AddTagEvent.trigger(AddTagEvent(context, tag)) }
                tag
            } else t
        }
    }

    suspend fun deleteTagWithMutex(context: Context, name: String) = lock.withLock { deleteTag(context, name) }
    suspend fun deleteTag(context: Context, name: String) {
        withContext(Dispatchers.Default) {
            val tag = tags.find { it.name == name }
            if (tag != null) {
                tags.remove(tag)
                tagDao.delete(tag)
                withContext(Dispatchers.Main) { DeleteTagEvent.trigger(DeleteTagEvent(context, tag)) }
            }
        }
    }

    suspend fun changeTagWithMutex(context: Context, changedTag: Tag) = lock.withLock { changeTag(context, changedTag) }
    suspend fun changeTag(context: Context, changedTag: Tag) {
        withContext(Dispatchers.Default) {
            val tag = tags.find { it.name == changedTag.name }
            if (tag != null) {
                tags.remove(tag)
                tags.add(changedTag)
                tagDao.update(changedTag)
                withContext(Dispatchers.Main) { ChangeTagEvent.trigger(ChangeTagEvent(context, tag, changedTag)) }
            }
        }
    }

    fun getSubscription(name: String) = subs.find { it.name == name }

    suspend fun addSubscriptionWithMutex(context: Context, sub: Subscription) = lock.withLock { addSubscription(context, sub) }
    suspend fun addSubscription(context: Context, sub: Subscription) {
        withContext(Dispatchers.Default) {
            if (getSubscription(sub.name) == null) {
                subs.add(sub)
                subDao.insert(sub)
                withContext(Dispatchers.Main) { AddSubEvent.trigger(AddSubEvent(context, sub)) }
            }
        }
    }

    suspend fun deleteSubscriptionWithMutex(context: Context, name: String) = lock.withLock { deleteSubscription(context, name) }
    suspend fun deleteSubscription(context: Context, name: String) {
        withContext(Dispatchers.Default) {
            val sub = subs.find { it.name == name }
            if (sub != null) {
                subs.remove(sub)
                subDao.delete(sub)
                withContext(Dispatchers.Main) { DeleteSubEvent.trigger(DeleteSubEvent(context, sub)) }
            }
        }
    }

    suspend fun changeSubscriptionWithMutex(context: Context, changedSub: Subscription) = lock.withLock { changeSubscription(context, changedSub) }
    suspend fun changeSubscription(context: Context, changedSub: Subscription) {
        withContext(Dispatchers.Default) {
            val sub = subs.find { it.name == changedSub.name }
            if (sub != null) {
                subs.remove(sub)
                subs.add(changedSub)
                subDao.update(changedSub)
                withContext(Dispatchers.Main) { ChangeSubEvent.trigger(ChangeSubEvent(context, sub, changedSub)) }
            }
        }
    }

    var limit = prefs.getInt("limit", 30)
        set(value) {
            field = value
            with(prefs.edit()) {
                putInt("limit", value)
                apply()
            }
        }

    var currentServerID = prefs.getInt("currentServer", 0)
        set(v) {
            field = v
            with(prefs.edit()) {
                putInt("currentServer", v)
                apply()
            }
        }
    var lastVersion = prefs.getInt("lastVersion", BuildConfig.VERSION_CODE)
        set(v) {
            field = v
            with(prefs.edit()) {
                putInt("lastVersion", v)
                apply()
            }
        }

    var nextServerID = prefs.getInt("nextServerID", 10) //10 because of potential default server
        get() {
            val i = field
            nextServerID = i + 1
            return i
        }
        set(v) {
            field = v
            with(prefs.edit()) {
                putInt("nextServerID", v)
                apply()
            }
        }


    var downloadOriginal = prefs.getBoolean("downloadOriginal", true)
        set(v) {
            field = v
            with(prefs.edit()) {
                putBoolean("downloadOriginal", v)
                apply()
            }
        }

    private var _savePathTested = false
    private var _saveFolder: DocumentFile = documentFile(context, prefs.getString("savePath", createDefaultSavePath())!!)
    fun getSaveFolder(context: Context): DocumentFile {
        if (!_savePathTested) {
            _savePathTested = true
            if (!_saveFolder.exists()) _saveFolder = documentFile(context, createDefaultSavePath())
        }
        return _saveFolder
    }

    fun setSaveFolder(file: DocumentFile) {
        _saveFolder = file
        with(prefs.edit()) {
            putString("savePath", file.uri.toString())
            apply()
        }
    }

    suspend fun deleteEverything() {
        withContext(Dispatchers.Default) {
            use {
                dropTable("tags", true)
                dropTable("subs", true)
                dropTable("servers", true)
            }
            withContext(Dispatchers.Main) {
                servers.clear()
                tags.clear()
                subs.clear()
            }
        }
        val p = prefs.all
        with(prefs.edit()) {
            for (prefToReset in p.entries)
                remove(prefToReset.key).apply()
        }
    }

    val tagDao = TagDao(context, this)
    val subDao = SubDao(context, this)
    val serverDao = ServerDao(this)
}

val Context.db: Database get() = Database.getDatabase(this)