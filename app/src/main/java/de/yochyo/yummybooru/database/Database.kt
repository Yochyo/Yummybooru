package de.yochyo.yummybooru.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import de.yochyo.booruapi.api.TagType
import de.yochyo.eventcollection.EventCollection
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
import de.yochyo.yummybooru.database.entities.TagCollectionWithTags
import de.yochyo.yummybooru.database.eventcollections.TagEventCollection
import de.yochyo.yummybooru.database.migrations.Migration3To4
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.utils.GlobalListeners
import de.yochyo.yummybooru.utils.enums.TagSortType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

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
        object : EventCollection<Server>(TreeSet(serverDao.selectAll())) {
            override fun remove(element: Server): Boolean {
                if (context.preferences.currentServerID == element.id) return false
                serverDao.delete(element)
                return super.remove(element)
            }

            override fun add(element: Server): Boolean {
                val id = serverDao.insert(element).toInt()
                if (id != -1) super.add(element.copy(id = id))
                return false
            }

            override fun replaceElement(old: Server, new: Server): Boolean {
                serverDao.update(new)
                return super.replaceElement(old, new)
            }
        }
    }

    val tags by lazy {
        object : EventCollection<Tag>(TagEventCollection(getTagComparator()).apply { addAll(tagDao.selectWhere(currentServer.id)) }) {
            override fun add(element: Tag): Boolean {
                val id = tagDao.insert(element).toInt()
                if (id != -1) super.add(element.copy(id = id))
                return false
            }

            override fun remove(element: Tag): Boolean {
                tagDao.delete(element)
                return super.remove(element)
            }

            override fun replaceElement(old: Tag, new: Tag): Boolean {
                tagDao.update(new)
                return super.replaceElement(old, new)
            }
        }
    }

    val tagCollections by lazy {
        object : EventCollection<TagCollectionWithTags>(ArrayList(tagCollectionDao.selectWhere(currentServer.id))) {
            override fun add(element: TagCollectionWithTags): Boolean {
                val id = tagCollectionDao.insertCollection(element.collection).toInt()
                if (id != -1) return super.add(TagCollectionWithTags(TagCollection(element.collection.name, element.collection.serverId, id), emptyList()))
                return false
            }

            override fun remove(element: TagCollectionWithTags): Boolean {
                tagCollectionDao.deleteCollection(element.collection)
                return super.remove(element)
            }

            override fun replaceElement(old: TagCollectionWithTags, new: TagCollectionWithTags): Boolean {
                tagCollectionDao.updateCollection(new.collection)
                return super.replaceElement(old, new)
            }
        }
    }

    fun addTagToCollection(collection: TagCollectionWithTags, tag: Tag) {
        collection.tags += tag
        tagCollectionDao.insertCollectionCrossRef(TagCollectionTagCrossRef(collection.collection.id, tag.id))
    }

    fun removeTagFromCollection(collection: TagCollectionWithTags, tag: Tag) {
        collection.tags -= tag
        tagCollectionDao.deleteCrossRef(TagCollectionTagCrossRef(collection.collection.id, tag.id))
    }

    suspend fun reloadTags() {
        withContext(Dispatchers.IO)
        {
            tags.replaceCollection(TagEventCollection(getTagComparator()).apply { addAll(tagDao.selectWhere(currentServer.id)) })
            tagCollections.replaceCollection(ArrayList(tagCollectionDao.selectWhere(currentServer.id)))
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
                _currentServer = getServer(context.preferences.currentServerID)
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
    fun getServer(id: Int) = servers.find { it.id == id }

    abstract val tagDao: TagDao
    abstract val serverDao: ServerDao
    abstract val tagCollectionDao: TagCollectionDao
}

val Context.db get() = RoomDb.getDb(this)