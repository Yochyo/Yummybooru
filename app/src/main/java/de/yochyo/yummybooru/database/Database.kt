package de.yochyo.yummybooru.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import org.jetbrains.anko.db.dropTable
import java.util.*


class Database(private val context: Context) : ManagedSQLiteOpenHelper(context, "db", null, 3) {
    val prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)

    val tagDao = TagDao(this)
    val serverDao = ServerDao(context, this)

    private val serverLock = Any()
    private var clearServerCache = true
    val servers = object : ObservingEventCollection<Server, Int>(TreeSet()) {
        override fun remove(element: Server): Boolean {
            return if (currentServerID == element.id) false
            else super.remove(element)
        }
    }
        get() = synchronized(serverLock) {
            if (clearServerCache) {
                field.replaceCollection(TreeSet(serverDao.selectAll()))
                clearServerCache = false
            }
            return field
        }

    private val tagLock = Any()
    private var clearTagCache = true
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
        get() = synchronized(tagLock) {
            if (clearTagCache) {
                field.replaceCollection(TreeSet(tagDao.selectWhere(currentServer)))
                clearTagCache = false
            }
            return field
        }

    companion object {
        private var instance: Database? = null
        fun getDatabase(context: Context): Database {
            if (instance == null)
                instance = Database(context)
            return instance!!
        }
    }

    fun clearCache() {
        clearServerCache()
        clearTagCache()
    }

    fun clearServerCache() = synchronized(clearTagCache) {
        clearServerCache = true
    }

    fun clearTagCache() = synchronized(clearServerCache) {
        clearTagCache = true
    }


    fun loadServer(server: Server) {
        val oldServer = currentServer
        currentServerID = server.id
        clearTagCache = true
        tags
        SelectServerEvent.trigger(SelectServerEvent(context, oldServer, server))
    }

    private var _currentServer: Server? = null
    val currentServer: Server
        get() {
            val s = _currentServer
            if (s == null || s.id != currentServerID) {
                _currentServer = getServer(currentServerID)
                    ?: if (servers.isNotEmpty()) servers.first() else null
            }

            return _currentServer ?: Server(context, "", "", "", "", "")
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
    var parallelBackgroundDownloads: Int
        get() = getPreference(context.getString(R.string.parallel_background_downloads), context.resources.getInteger(R.integer.parallel_background_downloads_default_value))
        set(value) = setPreference(context.getString(R.string.parallel_background_downloads), value)
    var previewColumns: Int
        get() = getPreference(context.getString(R.string.preview_columns), context.resources.getInteger(R.integer.preview_columns_default_value))
        set(value) = setPreference(context.getString(R.string.preview_columns), value)
    var previewStaggeredMode: Boolean
        get() = getPreference(context.getString(R.string.preview_staggered_mode), context.resources.getBoolean(R.bool.preview_staggered_mode_default_value))
        set(value) = setPreference(context.getString(R.string.preview_staggered_mode), value)
    var cropPreviewImage: Boolean
        get() = getPreference(context.getString(R.string.crop_preview), context.resources.getBoolean(R.bool.crop_preview_default_value))
        set(value) = setPreference(context.getString(R.string.crop_preview), value)

    var saveFolderUri: String
        get() = getPreference(context.getString(R.string.savePath), "")
        set(value) = setPreference(context.getString(R.string.savePath), value)
    var saveFolder: DocumentFile
        get() = DocumentFile.fromTreeUri(context, Uri.parse(saveFolderUri))!!
        set(value) {
            saveFolderUri = value.uri.toString()
        }

    var isFirstStart: Boolean
        get() = getPreference(context.getString(R.string.is_first_app_usage), context.resources.getBoolean(R.bool.is_first_app_usage_default_value))
        set(value) = setPreference(context.getString(R.string.is_first_app_usage), value)
    var useNomedia: Boolean
        get() = getPreference(context.getString(R.string.use_nomedia), context.resources.getBoolean(R.bool.use_nomedia_default_value))
        set(value) = setPreference(context.getString(R.string.use_nomedia), value)
    var hideDownloadToast: Boolean
        get() = getPreference(context.getString(R.string.hide_download_toast), context.resources.getBoolean(R.bool.hide_download_toast_default_value))
        set(value) = setPreference(context.getString(R.string.hide_download_toast), value)
    var combinedSearchSort: Int
        get() = getPreference(context.getString(R.string.combined_search_sort), context.resources.getInteger(R.integer.combined_search_sort_default_value))
        set(value) = setPreference(context.getString(R.string.combined_search_sort), value)

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
    }

    private fun getPreference(name: String, default: String) = prefs.getString(name, default)!!
    private fun getPreference(name: String, default: Int = 0) = prefs.getInt(name, default)
    private fun getPreference(name: String, default: Boolean = false) = prefs.getBoolean(name, default)
    fun setPreference(name: String, value: String) = with(prefs.edit()) {
        putString(name, value)
        apply()
    }

    fun setPreference(name: String, value: Int) = with(prefs.edit()) {
        putInt(name, value)
        apply()
    }

    fun setPreference(name: String, value: Boolean) = with(prefs.edit()) {
        putBoolean(name, value)
        apply()
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

    fun getTag(name: String) = tags.find { it.name == name }
    fun getServer(id: Int) = servers.find { it.id == id }
}

val Context.db: Database get() = Database.getDatabase(this)