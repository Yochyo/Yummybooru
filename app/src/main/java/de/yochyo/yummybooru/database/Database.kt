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
import de.yochyo.yummybooru.database.migrations.Migration4To5
import de.yochyo.yummybooru.utils.LiveDataValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@Database(entities = [Tag::class, Server::class, TagCollection::class, TagCollectionTagCrossRef::class], version = 5, exportSchema = true)
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
            }).addMigrations(Migration3To4(), Migration4To5()).build().apply { _db = this }
        }
    }

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
        addSource(_tagCollections) { update() }

        update()
    }


    val selectedServerLiveValue by lazy {
        LiveDataValue(selectedServer, null, runBlocking {
            GlobalScope.async {
                val servers = serverDao.selectAllNoFlow()
                servers.find { it.id == context.preferences.selectedServerId } ?: servers.firstOrNull()
            }.await()
        })
    }

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