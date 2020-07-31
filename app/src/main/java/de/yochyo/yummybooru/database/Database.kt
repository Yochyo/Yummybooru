package de.yochyo.yummybooru.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.documentfile.provider.DocumentFile
import de.yochyo.eventcollection.observablecollection.ObservingEventCollection
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.dao.ServerDao
import de.yochyo.yummybooru.database.dao.TagDao
import de.yochyo.yummybooru.database.utils.Upgrade
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.utils.GlobalListeners
import de.yochyo.yummybooru.utils.general.createDefaultSavePath
import de.yochyo.yummybooru.utils.general.currentServer
import de.yochyo.yummybooru.utils.general.documentFile
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import org.jetbrains.anko.db.dropTable
import java.util.*
import kotlin.collections.ArrayList


class Database(private val context: Context) : ManagedSQLiteOpenHelper(context, "db", null, 3) {
    private val prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)

    private val lock = Mutex(true)
    suspend fun join() {
        lock.withLock { }
    }

    val servers = object : ObservingEventCollection<Server, Int>(ArrayList()) {
        override fun remove(element: Server): Boolean {
            return if (currentServerID == element.id) false
            else super.remove(element)
        }
    }
    val tags = object : ObservingEventCollection<Tag, Int>(TreeSet()) {
        override fun add(element: Tag): Boolean {
            var isContained = false
            if (contains(element)) isContained = true
            else {
                element.isFavorite = !element.isFavorite
                if (contains(element)) isContained = true
                element.isFavorite = !element.isFavorite
            }
            return if (isContained) false else super.add(element)
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
            if (!lock.isLocked) lock.lock()
            GlobalListeners.unregisterGlobalListeners(context)
            val se: List<Server> = serverDao.selectAll()
            servers.clear()
            servers += se
            servers.notifyChange()
            loadServer(context.currentServer)
            lock.unlock()
        }
    }

    suspend fun loadServer(server: Server) {
        withContext(Dispatchers.IO) {
            val oldServer = context.currentServer
            GlobalListeners.unregisterGlobalListeners(context)
            withContext(Dispatchers.Main) { context.db.currentServerID = server.id }
            val t = tagDao.selectWhere(server)
            withContext(Dispatchers.Main) {
                tags.clear()
                tags += t
                tags.notifyChange()
            }
            GlobalListeners.registerGlobalListeners(context)
            SelectServerEvent.trigger(SelectServerEvent(context, oldServer, server))
        }
    }

    var limit: Int
        get() = getPreference(context.getString(R.string.page_size), context.getString(R.string.page_size_default_value)).toInt()
        set(value) = setPreference(context.getString(R.string.page_size), value.toString())


    var currentServerID: Int
        get() = getPreference(context.getString(R.string.currentServer), 0)
        set(value) = setPreference(context.getString(R.string.currentServer), value)

    var lastVersion: Int
        get() = getPreference(context.getString(R.string.lastVersion), BuildConfig.VERSION_CODE)
        set(value) = setPreference(context.getString(R.string.lastVersion), value)

    var downloadOriginal: Boolean
        get() = getPreference(context.getString(R.string.downloadOriginal), context.resources.getBoolean(R.bool.downloadOriginal_default_value))
        set(value) = setPreference(context.getString(R.string.downloadOriginal), value)

    var downloadWebm: Boolean
        get() = getPreference(context.getString(R.string.downloadWebm), context.resources.getBoolean(R.bool.downloadWebm_default_value))
        set(value) = setPreference(context.getString(R.string.downloadWebm), value)
    var preloadedImages: Int
        get() = getPreference(context.getString(R.string.preloaded_large_pictures), context.resources.getInteger(R.integer.preloaded_large_pictures_default_value))
        set(value) = setPreference(context.getString(R.string.preloaded_large_pictures), value)
    var clickToMoveToNextPicture: Boolean
        get() = getPreference(context.getString(R.string.click_to_move_to_next_image), context.resources.getBoolean(R.bool.click_to_move_to_next_image_default_value))
        set(value) = setPreference(context.getString(R.string.click_to_move_to_next_image), value)

    var saveFolder: DocumentFile
        get() = documentFile(context, getPreference(context.getString(R.string.savePath), createDefaultSavePath()))
        set(value) = setPreference(context.getString(R.string.savePath), value.uri.toString())

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

    private fun getPreference(name: String, default: String) = prefs.getString(name, default)!!
    private fun getPreference(name: String, default: Int = 0) = prefs.getInt(name, default)
    private fun getPreference(name: String, default: Boolean = false) = prefs.getBoolean(name, default)
    private fun setPreference(name: String, value: String) = with(prefs.edit()) {
        putString(name, value)
        apply()
    }

    private fun setPreference(name: String, value: Int) = with(prefs.edit()) {
        putInt(name, value)
        apply()
    }

    private fun setPreference(name: String, value: Boolean) = with(prefs.edit()) {
        putBoolean(name, value)
        apply()
    }

    val tagDao = TagDao(this)
    val serverDao = ServerDao(this)

    fun getTag(name: String) = tags.find { it.name == name }
    fun getServer(id: Int) = servers.find { it.id == id }
}

val Context.db: Database get() = Database.getDatabase(this)