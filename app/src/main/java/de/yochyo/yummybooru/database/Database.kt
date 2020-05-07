package de.yochyo.yummybooru.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.documentfile.provider.DocumentFile
import de.yochyo.eventcollection.observablecollection.ObservingEventCollection
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.dao.ServerDao
import de.yochyo.yummybooru.database.dao.TagDao
import de.yochyo.yummybooru.database.utils.Upgrade
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.utils.GlobalListeners
import de.yochyo.yummybooru.utils.general.Logger
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

    val servers = object : ObservingEventCollection<Server, Int>(ArrayList()) {
        override fun remove(element: Server): Boolean {
            return if (currentServerID == element.id) false
            else super.remove(element)
        }
    }
    val tags = object : ObservingEventCollection<Tag, Int>(TreeSet<Tag>()) {
        override fun add(element: Tag): Boolean {
            var isContained = false
            if (contains(element)) isContained = true
            else {
                element.isFavorite = !element.isFavorite
                if (contains(element)) isContained = true
                element.isFavorite = !element.isFavorite
            }
            return if(isContained) true else super.add(element)
        }
    }

    companion object {
        private var instance: Database? = null
        fun getDatabase(context: Context): Database {
            if (instance == null) {
                instance = Database(context)
                GlobalScope.launch {
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
            GlobalListeners.unregisterGlobalListeners(context)
            val se: List<Server> = serverDao.selectAll()
            servers.clear()
            servers += se
            servers.notifyChange()
            loadServer(context.currentServer)
        }
    }

    suspend fun loadServer(server: Server) {
        withContext(Dispatchers.IO) {
            GlobalListeners.unregisterGlobalListeners(context)
            withContext(Dispatchers.Main) { context.db.currentServerID = server.id }
            val t = tagDao.selectWhere(server)
            withContext(Dispatchers.Main) {
                tags.clear()
                tags += t
                tags.notifyChange()
            }
            GlobalListeners.registerGlobalListeners(context)
            SelectServerEvent.trigger(SelectServerEvent(context, context.currentServer, server))
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
    var downloadWebm = prefs.getBoolean("downloadWebm", true)
        set(value) {
            field = value
            with(prefs.edit()) {
                putBoolean("downloadWebm", value)
                apply()
            }
        }

    private var _savePathTested = false
    var saveFolder: DocumentFile = documentFile(context, prefs.getString("savePath", createDefaultSavePath())!!)
        get() {
            if (!_savePathTested) {
                _savePathTested = true
                if (!field.exists()) {
                    val uri = field.uri
                    Logger.log("$uri does not exist anymore")
                    field = documentFile(context, createDefaultSavePath())
                }
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
            GlobalListeners.unregisterGlobalListeners(context)
            servers.clear()
            tags.clear()
            use {
                dropTable("tags", true)
                dropTable("servers", true)
                tagDao.createTable(this)
                serverDao.createTable(this)
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
}

val Context.db: Database get() = Database.getDatabase(this)