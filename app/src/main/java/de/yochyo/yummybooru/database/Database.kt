package de.yochyo.yummybooru.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.documentfile.provider.DocumentFile
import de.yochyo.eventcollection.ObservingEventCollection
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.events.OnRemoveElementsEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.dao.ServerDao
import de.yochyo.yummybooru.database.dao.TagDao
import de.yochyo.yummybooru.database.utils.Upgrade
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.utils.general.createDefaultSavePath
import de.yochyo.yummybooru.utils.general.currentServer
import de.yochyo.yummybooru.utils.general.documentFile
import kotlinx.coroutines.*
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import org.jetbrains.anko.db.dropTable
import java.util.*
import kotlin.collections.ArrayList


class Database(private val context: Context) : ManagedSQLiteOpenHelper(context, "db", null, 3) {
    private val prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)
    private val listeners = DatabaseListeners()

    val servers = object : ObservingEventCollection<Server, Int>(ArrayList()) {
        override fun remove(element: Server): Boolean {
            return if (currentServerID == element.id) false
            else super.remove(element)
        }
    }
    val tags = ObservingEventCollection(TreeSet<Tag>())

    companion object {
        private var instance: Database? = null
        fun getDatabase(context: Context): Database {
            if (instance == null) {
                instance = Database(context)
                GlobalScope.launch {
                    delay(100)
                    instance!!.loadDatabase()
                }
            }
            return instance!!
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        serverDao.createTable(db)
        tagDao.createTable(db)

        db.execSQL("INSERT INTO servers VALUES ('Danbooru', 'https://danbooru.donmai.us/', 'danbooru', '', '', NULL);", emptyArray())
        db.execSQL("INSERT INTO servers VALUES ('Konachan', 'https://konachan.com/', 'moebooru', '', '', NULL);", emptyArray())
        db.execSQL("INSERT INTO servers VALUES ('Yande.re', 'https://yande.re/', 'moebooru', '', '', NULL);", emptyArray())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Upgrade.upgradeFromTo(db, oldVersion, newVersion)
    }

    suspend fun loadDatabase() {
        withContext(Dispatchers.IO) {
            val se: List<Server> = serverDao.selectAll()
            servers.clear()
            servers.addAll(se)
            loadServer(context.currentServer)
        }
    }

    suspend fun loadServer(server: Server) {
        withContext(Dispatchers.IO) {
            listeners.unregisterListeners()
            withContext(Dispatchers.Main) { context.db.currentServerID = server.id }
            tags.clear()
            val t = tagDao.selectWhere(server)
            withContext(Dispatchers.Main) { tags += t }
            listeners.registerListeners()
            SelectServerEvent.trigger(SelectServerEvent(context, context.currentServer, server))
            server.updateMissingTypeTags(context)
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

    var downloadOriginal = prefs.getBoolean("downloadOriginal", true)
        set(v) {
            field = v
            with(prefs.edit()) {
                putBoolean("downloadOriginal", v)
                apply()
            }
        }

    private var _savePathTested = false
    var saveFolder: DocumentFile = documentFile(context, prefs.getString("savePath", createDefaultSavePath())!!)
        get() {
            if (!_savePathTested) {
                _savePathTested = true
                if (!field.exists()) field = documentFile(context, createDefaultSavePath())
            }
            return field
        }
        set(value) {
            field = value
            with(prefs.edit()) {
                putString("savePath", value.uri.toString())
                apply()
            }
        }

    suspend fun deleteEverything() {
        withContext(Dispatchers.Default) {
            use {
                dropTable("tags", true)
                dropTable("servers", true)
            }
            withContext(Dispatchers.Main) {
                servers.clear()
                tags.clear()
            }
        }
        val p = prefs.all
        with(prefs.edit()) {
            for (prefToReset in p.entries)
                remove(prefToReset.key).apply()
        }
    }

    val tagDao = TagDao(this)
    val serverDao = ServerDao(this)

    fun getTag(name: String) = tags.find { it.name == name }
    fun getServer(id: Int) = servers.find { it.id == id }

    private inner class DatabaseListeners {
        //The listeners in this class automatically update the database when the ObservingEventCollection changes
        private val addServerListener = Listener.create<OnAddElementsEvent<Server>> {
            GlobalScope.launch(Dispatchers.IO) {
                for (server in it.elements)
                    server.id = serverDao.insert(server)
            }
        }
        private val removeServerListener = Listener.create<OnRemoveElementsEvent<Server>> { GlobalScope.launch(Dispatchers.IO) { it.elements.forEach { element -> serverDao.delete(element) } } }
        private val changeServerListener = Listener.create<OnChangeObjectEvent<Server, Int>> { GlobalScope.launch(Dispatchers.IO) { serverDao.update(it.new) } }

        private val addTagListener = Listener.create<OnAddElementsEvent<Tag>> {
            GlobalScope.launch(Dispatchers.IO) {
                val id = context.currentServer.id
                it.elements.forEach { element ->
                    tagDao.insert(element.apply { serverID = id })
                }
            }
        }
        private val removeTagListener = Listener.create<OnRemoveElementsEvent<Tag>> { GlobalScope.launch(Dispatchers.IO) { it.elements.forEach { element -> tagDao.delete(element) } } }
        private val changeTagListener = Listener.create<OnChangeObjectEvent<Tag, Int>> { GlobalScope.launch(Dispatchers.IO) { tagDao.update(it.new) } }

        fun registerListeners() {
            servers.onAddElements.registerListener(addServerListener)
            servers.onRemoveElements.registerListener(removeServerListener)
            servers.onElementChange.registerListener(changeServerListener)

            tags.onAddElements.registerListener(addTagListener)
            tags.onRemoveElements.registerListener(removeTagListener)
            tags.onElementChange.registerListener(changeTagListener)
        }

        fun unregisterListeners() {
            servers.onAddElements.removeListener(addServerListener)
            servers.onRemoveElements.removeListener(removeServerListener)
            servers.onElementChange.removeListener(changeServerListener)

            tags.onAddElements.removeListener(addTagListener)
            tags.onRemoveElements.removeListener(removeTagListener)
            tags.onElementChange.removeListener(changeTagListener)
        }
    }
}

val Context.db: Database get() = Database.getDatabase(this)