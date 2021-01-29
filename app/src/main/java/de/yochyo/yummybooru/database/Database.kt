package de.yochyo.yummybooru.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import de.yochyo.booruapi.api.TagType
import de.yochyo.eventcollection.observablecollection.ObservingEventCollection
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.dao.ServerDao
import de.yochyo.yummybooru.database.dao.TagDao
import de.yochyo.yummybooru.database.eventcollections.TagEventCollection
import de.yochyo.yummybooru.database.utils.Upgrade
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.utils.GlobalListeners
import de.yochyo.yummybooru.utils.enums.TagSortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper
import org.jetbrains.anko.db.dropTable
import java.util.*


class Database(private val context: Context) : ManagedSQLiteOpenHelper(context, "db", null, 3) {
    val prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)

    val tagDao = TagDao(this)
    val serverDao = ServerDao(context, this)

    val servers by lazy {
        object : ObservingEventCollection<Server, Int>(TreeSet(serverDao.selectAll())) {
            override fun remove(element: Server): Boolean {
                return if (currentServerID == element.id) false
                else super.remove(element)
            }
        }
    }

    val tags by lazy {
        ObservingEventCollection(TagEventCollection(getTagComparator()).apply { addAll(TreeSet(tagDao.selectWhere(currentServer))) })
    }

    companion object {
        private var instance: Database? = null
        fun getDatabase(context: Context): Database {
            if (instance == null)
                instance = Database(context)
            return instance!!
        }
    }

    suspend fun reloadTags() {
        withContext(Dispatchers.IO) {
            val t = TagEventCollection(getTagComparator()).apply { addAll(TreeSet(tagDao.selectWhere(currentServer))) }
            this@Database.tags.replaceCollection(t)
        }
    }

    suspend fun reloadServers() {
        withContext(Dispatchers.IO) {
            this@Database.servers.replaceCollection(TreeSet(serverDao.selectAll()))
        }
    }

    suspend fun reloadDB() {
        reloadServers()
        reloadTags()
    }

    suspend fun loadServer(server: Server) {
        val oldServer = currentServer
        currentServerID = server.id
        reloadTags()
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
    var sortTagsByFavoriteFirst: Boolean
        get() = getPreference(context.getString(R.string.show_favorites_first), context.resources.getBoolean(R.bool.show_favorites_first_default_value))
        set(value) = setPreference(context.getString(R.string.show_favorites_first), value)
    var sortTagsByTagType: Boolean
        get() = getPreference(context.getString(R.string.sort_by_tag_type), context.resources.getBoolean(R.bool.sort_by_tag_type_default_value))
        set(value) = setPreference(context.getString(R.string.sort_by_tag_type), value)
    var tagSortType: TagSortType
        get() = TagSortType.fromValue(
            getPreference(
                context.getString(R.string.sort_tag_comparator),
                context.resources.getString(R.string.sort_tag_comparator_default_value)
            ).toInt()
        )
        set(value) = setPreference(context.getString(R.string.sort_tag_comparator), value.value.toString())

    fun getTagComparator(): Comparator<Tag> {
        fun TagType.toInt() = when (this) {
            TagType.ARTIST -> 0
            TagType.COPYRIGHT -> 1
            TagType.CHARACTER -> 2
            TagType.GENERAL -> 3
            TagType.META -> 4
            TagType.UNKNOWN -> 5
        }

        val comparatorChain = ArrayList<Comparator<Tag>>()
        if (sortTagsByFavoriteFirst) comparatorChain += Comparator { o1, o2 ->
            if (o1.isFavorite == o2.isFavorite) 0
            else if (o1.isFavorite && !o2.isFavorite) -1
            else 1
        }

        if (sortTagsByTagType) comparatorChain += Comparator { o1, o2 -> o1.type.toInt().compareTo(o2.type.toInt()) }
        comparatorChain += when (tagSortType) {
            TagSortType.NAME_ASC -> Comparator { o1, o2 -> o1.name.compareTo(o2.name) }
            TagSortType.NAME_DES -> Comparator { o1, o2 -> o2.name.compareTo(o1.name) }
            TagSortType.DATE_ASC -> Comparator { o1, o2 -> o2.creation.compareTo(o1.creation) }
            TagSortType.DATE_DES -> Comparator { o1, o2 -> o2.creation.compareTo(o1.creation) }
        }
        return Comparator { o1, o2 ->
            var res = 0
            for (comparator in comparatorChain) {
                res = comparator.compare(o1, o2)
                if (res != 0) break
            }
            res
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