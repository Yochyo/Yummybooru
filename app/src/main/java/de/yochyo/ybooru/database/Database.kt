package de.yochyo.ybooru.database

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.arch.persistence.room.migration.Migration
import android.content.Context
import android.content.SharedPreferences
import de.yochyo.ybooru.database.converter.DateConverter
import de.yochyo.ybooru.database.entities.Subscription
import de.yochyo.ybooru.database.entities.SubscriptionDao
import de.yochyo.ybooru.database.entities.Tag
import de.yochyo.ybooru.database.entities.TagDao
import de.yochyo.ybooru.database.liveData.LiveTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@android.arch.persistence.room.Database(entities = [Tag::class, Subscription::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class Database : RoomDatabase() {
    private lateinit var prefs: SharedPreferences

    companion object {
        var instance: Database? = null
        fun initDatabase(context: Context): Database {
            if (instance == null) instance = Room.databaseBuilder(context.applicationContext,
                    Database::class.java, "database").addMigrations(*Migrations.all).build()
            instance!!.prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)
            instance!!.initialize()
            return instance!!
        }

    }

    val tags = LiveTree<Tag>()
    val subs = LiveTree<Subscription>()

    fun initialize() {
        GlobalScope.launch {
            val t = tagDao.getAllTags()
            val s = subDao.getAllSubscriptions()
            GlobalScope.launch(Dispatchers.Main) {
                tags += t
                subs += s
            }
        }
    }

    fun getTag(name: String) = tags.find { it.name == name }
    fun addTag(tag: Tag) {
        if (getTag(tag.name) == null) {
            tags += tag
            GlobalScope.launch { tagDao.insert(tag) }
        }
    }

    fun deleteTag(name: String) {
        val tag = tags.find { it.name == name }
        if (tag != null) {
            tags.remove(tag)
            GlobalScope.launch { tagDao.delete(tag) }
        }
    }

    fun changeTag(changedTag: Tag): Boolean {
        val tag = tags.find { it.name == changedTag.name }
        return if (tag != null) {
            tags.remove(tag)
            tags.add(changedTag)
            GlobalScope.launch { tagDao.update(changedTag) }
            true
        } else false
    }

    fun getSubscription(name: String) = subs.find { it.name == name }
    fun addSubscription(sub: Subscription) {
        if (getSubscription(sub.name) == null) {
            subs += sub
            GlobalScope.launch { subDao.insert(sub) }
        }
    }

    fun deleteSubscription(name: String) {
        val sub = subs.find { it.name == name }
        if (sub != null) {
            subs.remove(sub)
            GlobalScope.launch { subDao.delete(sub) }
        }
    }

    fun changeSubscription(changedSub: Subscription): Boolean {
        val sub = subs.find { it.name == changedSub.name }
        return if (sub != null) {
            subs.remove(sub)
            subs.add(changedSub)
            GlobalScope.launch { subDao.update(changedSub) }
            true
        } else false
    }

    private var _limit: Int? = null
    var limit: Int
        get() {
            if (_limit == null) _limit = prefs.getInt("limit", 30)
            return _limit!!
        }
        set(value) {
            _limit = value
            with(prefs.edit()) {
                putInt("limit", value)
                apply()
            }
        }

    private var _r18: Boolean? = null
    var r18: Boolean
        get() {
            if (_r18 == null) _r18 = prefs.getBoolean("r18", false)
            return _r18!!
        }
        set(value) {
            _r18 = value
            with(prefs.edit()) {
                putBoolean("r18", value)
                apply()
            }
        }

    private var _sortTags: String? = null
    var sortTags: String
        get() {
            if (_sortTags == null) _sortTags = prefs.getString("sortTags", "00")
            return _sortTags!!
        }
        set(value) {
            _sortTags = value
            with(prefs.edit()) {
                putString("sortTags", value)
                apply()
            }
        }
    private var _sortSubs: String? = null
    var sortSubs: String
        get() {
            if (_sortSubs == null) _sortSubs = prefs.getString("sortSubs", "00")
            return _sortSubs!!
        }
        set(value) {
            _sortSubs = value
            with(prefs.edit()) {
                putString("sortSubs", value)
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

    abstract val subDao: SubscriptionDao
    abstract val tagDao: TagDao
}

val database: Database
    get() = Database.instance!!


object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
        }
    }


    val all = arrayOf(MIGRATION_1_2)
}