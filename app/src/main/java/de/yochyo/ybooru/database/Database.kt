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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

@android.arch.persistence.room.Database(entities = [Tag::class, Subscription::class], version = 1)
@TypeConverters(DateConverter::class)
abstract class Database : RoomDatabase() {
    private lateinit var prefs: SharedPreferences

    companion object {
        var _instance: Database? = null
        fun initDatabase(context: Context): Database {
            if (_instance == null) _instance = Room.databaseBuilder(context.applicationContext,
                    Database::class.java, "database").addMigrations(*Migrations.all).build()
            _instance!!.prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)
            return _instance!!
        }
    }

    val tags = TreeSet<Tag>()
        get() {
            if (field.isEmpty())
                runBlocking {
                    val job = GlobalScope.launch { field += tagDao.getAllTags() }
                    job.join()
                }
            return field
        }
    val subs = TreeSet<Subscription>()
        get() {
            if (field.isEmpty())
                runBlocking {
                    val job = GlobalScope.launch { field += subDao.getAllSubscriptions() }
                    job.join()
                }
            return field
        }

    fun getTag(name: String) = tags.find { it.name == name }
    fun addTag(tag: Tag) {
        tags += tag
        GlobalScope.launch { tagDao.insert(tag) }
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
        subs += sub
        GlobalScope.launch { subDao.insert(sub) }
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

val Context.initDatabase: Database
    get() = Database.initDatabase(this)
val database: Database
    get() = Database._instance!!


object Migrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
        }
    }


    val all = arrayOf(MIGRATION_1_2)
}