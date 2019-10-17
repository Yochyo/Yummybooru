package de.yochyo.yummybooru.database

import android.content.Context
import android.content.SharedPreferences
import androidx.documentfile.provider.DocumentFile
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import de.yochyo.eventmanager.EventCollection
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.api.entities.*
import de.yochyo.yummybooru.database.converter.DateConverter
import de.yochyo.yummybooru.events.events.*
import de.yochyo.yummybooru.utils.createDefaultSavePath
import de.yochyo.yummybooru.utils.documentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.collections.ArrayList


@androidx.room.Database(entities = [Tag::class, Subscription::class, Server::class], version = 2)
@TypeConverters(DateConverter::class)
abstract class Database : RoomDatabase() {
    private val lock = Mutex()

    companion object {
        private lateinit var context: Context
        private var _prefs: SharedPreferences? = null
        val prefs: SharedPreferences get() = _prefs!!

        var instance: Database? = null
        fun initDatabase(context: Context): Database {
            this.context = context
            if (instance == null) {
                _prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)
                instance = Room.databaseBuilder(context.applicationContext,
                        Database::class.java, "db")
                        .addCallback(object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                for (s in DefaultServerExeq.all)
                                    db.execSQL(s)
                            }
                        }).addMigrations(*Migrations.all).build()
                instance!!.servers.onUpdate.registerListener { GlobalScope.launch(Dispatchers.Main) { UpdateServersEvent.trigger(UpdateServersEvent(context, instance!!.servers)) } }
                instance!!.tags.onUpdate.registerListener { GlobalScope.launch(Dispatchers.Main) { UpdateTagsEvent.trigger(UpdateTagsEvent(context, instance!!.tags)) } }
                instance!!.subs.onUpdate.registerListener { GlobalScope.launch(Dispatchers.Main) { UpdateSubsEvent.trigger(UpdateSubsEvent(context, instance!!.subs)) } }
                GlobalScope.launch {
                    instance!!.loadServers()
                    Server.currentServer.select(context)
                }
            }
            return instance!!
        }
    }

    val servers: EventCollection<Server> = EventCollection(TreeSet())
    val tags: EventCollection<Tag> = EventCollection(TreeSet())
    val subs: EventCollection<Subscription> = EventCollection(TreeSet())
    private suspend fun loadServers() {
        withContext(Dispatchers.Default) {
            val se: List<Server> = serverDao.getAllServers()
            servers.clear()
            servers.addAll(se)
        }
    }

    suspend fun loadServerWithMutex(context: Context) = lock.withLock { loadServer(context) }
    suspend fun loadServer(context: Context) {
        withContext(Dispatchers.Default) {
            val server = Server.currentServer
            loadTags(context, server.id)
            loadSubscriptions(context, server.id)
        }
    }

    suspend fun loadTagsWithMutex(context: Context, serverID: Int) = lock.withLock { loadTags(context, serverID) }
    suspend fun loadTags(context: Context, serverID: Int) {
        withContext(Dispatchers.Default) {
            val t = tagDao.getAllTags().filter { it.serverID == serverID }
            withContext(Dispatchers.Main) {
                tags.clear()
                tags.addAll(t)
            }
        }
    }

    suspend fun loadSubscriptionsWithMutex(context: Context, serverID: Int) = lock.withLock { loadSubscriptions(context, serverID) }
    suspend fun loadSubscriptions(context: Context, serverID: Int) {
        withContext(Dispatchers.Default) {
            val s = subDao.getAllSubscriptions().filter { it.serverID == serverID }
            subs.clear()
            subs.addAll(s)
        }
    }

    fun getServer(id: Int) = servers.find { it.id == id }

    suspend fun addServerWithMutex(context: Context, server: Server, id: Int = nextServerID++) = lock.withLock { addServer(context, server, id) }
    suspend fun addServer(context: Context, server: Server, id: Int = nextServerID++) {
        withContext(Dispatchers.Default) {
            val s = getServer(server.id)
            if (s == null) {
                servers.add(server.copy(id = id))
                serverDao.insert(server)
                withContext(Dispatchers.Main) { AddServerEvent.trigger(AddServerEvent(context, server)) }
            }
        }
    }

    suspend fun deleteServerWithMutex(context: Context, id: Int) = lock.withLock { deleteServer(context, id) }
    suspend fun deleteServer(context: Context, id: Int) {
        withContext(Dispatchers.Default) {
            val s = servers.find { id == it.id }
            if (s != null && !s.isSelected) {
                servers.remove(s)
                serverDao.delete(s)
                for (tag in tagDao.getAllTags().filter { it.serverID == s.id }) tagDao.delete(tag)
                for (sub in subDao.getAllSubscriptions().filter { it.serverID == s.id }) subDao.delete(sub)
                withContext(Dispatchers.Main) { DeleteServerEvent.trigger(DeleteServerEvent(context, s)) }
            }
        }
    }

    suspend fun changeServerWithMutex(context: Context, changedServer: Server) = lock.withLock { changeServer(context, changedServer) }
    suspend fun changeServer(context: Context, changedServer: Server) {
        withContext(Dispatchers.Default) {
            val s = servers.find { it.id == changedServer.id }
            if (s != null) {
                val wasCurrentServer = Server.currentServer == changedServer
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

    var nextServerID = prefs.getInt("nextServerID", DefaultServerExeq.all.size)
        set(v) {
            field = v
            with(prefs.edit()) {
                putInt("nextServerID", v)
                apply()
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


    var sortTags = prefs.getString("sortTags", "00")!!
        set(value) {
            field = value
            with(prefs.edit()) {
                putString("sortTags", value)
                apply()
            }
        }
    var sortSubs = prefs.getString("sortSubs", "00")!!
        set(value) {
            field = value
            with(prefs.edit()) {
                putString("sortSubs", value)
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

    var saveFile: DocumentFile = documentFile(context, prefs.getString("savePath", createDefaultSavePath()))
    set(v){
        field = v
        with(prefs.edit()){
            putString("savePath", v.uri.toString())
            apply()
        }
    }

    var sortTagsByFavorite: Boolean
        get() = sortTags.first() == '1'
        set(v) {
            sortTags = "${sortTags.first()}$v"
        }
    var sortTagsByAlphabet: Boolean
        get() = sortTags.last() == '1'
        set(v) {
            sortTags = "$v${sortTags.last()}"
        }

    var sortSubsByFavorite: Boolean
        get() = sortSubs.first() == '1'
        set(v) {
            sortSubs = "${sortSubs.first()}$v"
        }
    var sortSubsByAlphabet: Boolean
        get() = sortSubs.last() == '1'
        set(v) {
            sortSubs = "$v${sortSubs.last()}"
        }

    suspend fun deleteEverything() {
        withContext(Dispatchers.Default) {
            clearAllTables()
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

    abstract val subDao: SubscriptionDao
    abstract val tagDao: TagDao
    abstract val serverDao: ServerDao
}

val db: Database get() = Database.instance!!


private object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
        }
    }


    val all = arrayOf(MIGRATION_1_2)
}

object DefaultServerExeq {
    val all = ArrayList<String>()

    init {
        all += "INSERT INTO servers (name,api,url,userName,password,enableR18Filter,id) VALUES ('Danbooru', 'danbooru', 'https://danbooru.donmai.us/', '', '', 0, 0);"
        all += "INSERT INTO servers (name,api,url,userName,password,enableR18Filter,id) VALUES ('Konachan', 'moebooru', 'https://konachan.com/', '', '', 0, 1);"
        all += "INSERT INTO servers (name,api,url,userName,password,enableR18Filter,id) VALUES ('Yande.re', 'moebooru', 'https://yande.re/', '', '', 0, 2);"
    }
}