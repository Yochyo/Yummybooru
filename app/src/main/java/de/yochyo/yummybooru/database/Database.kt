package de.yochyo.yummybooru.database

import android.content.Context
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import de.yochyo.yummybooru.R
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
import de.yochyo.yummybooru.database.migrations.Migration3To4
import de.yochyo.yummybooru.utils.LiveDataValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(entities = [Tag::class, Server::class, TagCollection::class, TagCollectionTagCrossRef::class], version = 4, exportSchema = true)
@TypeConverters(ConvertDate::class, ConvertBoolean::class, ConvertTagType::class)
abstract class RoomDb : RoomDatabase(), DaoMethods {
    override val db: RoomDb = this

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
            }).addMigrations(Migration3To4()).build().apply { _db = this }
        }
    }

    /*
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
     */

    private val currentServerId = context.preferences.prefs.intLiveData(context.getString(R.string.currentServer), 1)
    private val _tags = tagDao.selectAll()
    private val _tagCollections = tagCollectionDao.selectAll()

    val servers = serverDao.selectAll()

    val selectedServer: LiveData<Server> = MediatorLiveData<Server>().apply {
        fun update() {
            val id = currentServerId.value ?: return
            val servers = servers.value ?: return
            value = servers.find { it.id == id } ?: servers.firstOrNull()
        }
        addSource(currentServerId) { update() }
        addSource(servers) { update() }

        update()
    }.distinctUntilChanged()

    val tags = MediatorLiveData<List<Tag>>().apply {
        fun update() {
            val serverId = selectedServer.value?.id ?: return
            val tags = _tags.value ?: return
            value = tags.filter { it.serverId == serverId }
        }
        addSource(selectedServer) { update() }
        addSource(_tags) { update() }

        update()
    }

    val tagCollections = MediatorLiveData<List<TagCollectionWithTags>>().apply {
        fun update() {
            val serverId = selectedServer.value?.id ?: return
            val tags = _tagCollections.value ?: return
            value = tags.filter { it.collection.serverId == serverId }
        }
        addSource(selectedServer) { update() }
        addSource(_tags) { update() }

        update()
    }

    val selectedServerLiveValue by lazy { LiveDataValue(selectedServer, null) }
    val selectedServerValue get() = selectedServerLiveValue.value

    suspend fun deleteEverything() {
        withContext(Dispatchers.IO) {
            clearAllTables()
        }
    }


    abstract val tagDao: TagDao
    abstract val serverDao: ServerDao
    abstract val tagCollectionDao: TagCollectionDao
}

val Context.db get() = RoomDb.getDb(this)
val View.db get() = this.context.db