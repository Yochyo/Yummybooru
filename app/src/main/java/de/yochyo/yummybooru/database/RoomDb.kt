package de.yochyo.yummybooru.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import de.yochyo.booruapi.api.TagType
import de.yochyo.eventcollection.observablecollection.ObservingEventCollection
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.converter.ConvertBoolean
import de.yochyo.yummybooru.database.converter.ConvertDate
import de.yochyo.yummybooru.database.converter.ConvertTagType
import de.yochyo.yummybooru.database.dao.ServerDao
import de.yochyo.yummybooru.database.dao.TagCollectionDao
import de.yochyo.yummybooru.database.dao.TagDao
import de.yochyo.yummybooru.database.entities.TagCollection
import de.yochyo.yummybooru.database.entities.TagCollectionTagCrossRef
import de.yochyo.yummybooru.database.eventcollections.TagEventCollection
import de.yochyo.yummybooru.database.migrations.Migration3To4
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.utils.GlobalListeners
import de.yochyo.yummybooru.utils.enums.TagSortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

@Database(entities = [Tag::class, Server::class, TagCollection::class, TagCollectionTagCrossRef::class], version = 4, exportSchema = true)
@TypeConverters(ConvertDate::class, ConvertBoolean::class, ConvertTagType::class)
abstract class RoomDb : RoomDatabase() {
    companion object {
        private lateinit var context: Context
        private var _db: RoomDb? = null
        fun getDb(context: Context): RoomDb {
            this.context = context
            return _db ?: Room.databaseBuilder(context.applicationContext, RoomDb::class.java, "db").addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    db.execSQL("INSERT INTO servers VALUES ('Danbooru', 'https://danbooru.donmai.us/', 'danbooru', '', '', NULL);", emptyArray())
                    db.execSQL("INSERT INTO servers VALUES ('Konachan', 'https://konachan.com/', 'moebooru', '', '', NULL);", emptyArray())
                    db.execSQL("INSERT INTO servers VALUES ('Yande.re', 'https://yande.re/', 'moebooru', '', '', NULL);", emptyArray())
                }
            }).allowMainThreadQueries().addMigrations(Migration3To4()).build().apply { _db = this }
        }
    }

    val servers by lazy {
        object : ObservingEventCollection<Server, Int>(TreeSet(serverDao.selectAll())) {
            override fun remove(element: Server): Boolean {
                return if (context.preferences.currentServerID == element.id) false
                else super.remove(element)
            }
        }
    }

    val tags by lazy {
        val a = ObservingEventCollection(TagEventCollection(getTagComparator()).apply { addAll(tagDao.selectWhere(currentServer.id)) })
        println()
        a
    }

    suspend fun reloadTags() {
        withContext(Dispatchers.IO) {
            tags.replaceCollection(TagEventCollection(getTagComparator()).apply { tagDao.selectWhere(currentServer.id) })
        }
    }

    suspend fun reloadServers() = withContext(Dispatchers.IO) { servers.replaceCollection(TreeSet(serverDao.selectAll())) }

    suspend fun reloadDB() {
        reloadServers()
        reloadTags()
    }

    suspend fun loadServer(server: Server) {
        val oldServer = currentServer
        context.preferences.currentServerID = server.id
        reloadTags()
        SelectServerEvent.trigger(SelectServerEvent(context, oldServer, server))
    }

    private var _currentServer: Server? = null
    val currentServer: Server
        get() {
            val s = _currentServer
            if (s == null || s.id != context.preferences.currentServerID) {
                _currentServer = getServer(context, context.preferences.currentServerID)
                    ?: if (servers.isNotEmpty()) servers.first() else null
            }

            return _currentServer ?: Server("", "", "", "", "")
        }


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
        if (context.preferences.sortTagsByFavoriteFirst) comparatorChain += Comparator { o1, o2 ->
            if (o1.isFavorite == o2.isFavorite) 0
            else if (o1.isFavorite && !o2.isFavorite) -1
            else 1
        }

        if (context.preferences.sortTagsByTagType) comparatorChain += Comparator { o1, o2 -> o1.type.toInt().compareTo(o2.type.toInt()) }
        comparatorChain += when (context.preferences.tagSortType) {
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
        withContext(Dispatchers.IO) {
            GlobalListeners.unregisterGlobalListeners(context)
            servers.clear()
            tags.clear()
            clearAllTables()
        }
    }

    fun getTag(name: String) = tags.find { it.name == name }
    fun getServer(context: Context, id: Int) = servers.find { it.id == id }

    abstract val tagDao: TagDao
    abstract val serverDao: ServerDao
    abstract val tagCollectionDao: TagCollectionDao
}

val Context.db get() = RoomDb.getDb(this)