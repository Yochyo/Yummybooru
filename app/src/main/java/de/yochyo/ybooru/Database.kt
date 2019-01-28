package de.yochyo.ybooru

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import de.yochyo.ybooru.api.Subscription
import de.yochyo.ybooru.api.Tag
import org.jetbrains.anko.db.*
import java.text.DateFormat
import java.util.*
import kotlin.collections.ArrayList

abstract class Database(context: Context) : ManagedSQLiteOpenHelper(context, "database", null, 1) {
    private val prefs = context.getSharedPreferences("default.xml", Context.MODE_PRIVATE)

    private val TABLE_TAGS = "tags"
    private val TABLE_SUBSCRIPTION = "subs"
    private val COLUMN_NAME = "name"
    private val COLUMN_IS_FAVORITE = "isFavorite"
    private val COLUMN_TYPE = "type"
    private val COLUMN_DATE = "Date"
    private val COLUMN_SUBSCRIBED_SINCE = "start"
    private val COLUMN_SUBSCRIBED_STATUS = "current"

    companion object {
        private var _instance: Database? = null
        fun getInstance(context: Context): Database {
            if (_instance == null) _instance = object : Database(context) {}
            return _instance!!
        }
    }

    private val tags = ArrayList<Tag>()
    private val subs = ArrayList<Subscription>()

    init {
        tags += getTags()
        subs += getSubscriptions()
    }


    fun addTag(name: String, type: String, isFavorite: Boolean): Tag? {
        val date = Date()
        val tag = Tag(name, type, isFavorite, date)
        if (tags.find { it.name == tag.name } == null) {
            tags += tag
            use {
                insert(TABLE_TAGS,
                        COLUMN_NAME to tag.name,
                        COLUMN_IS_FAVORITE to if (tag.isFavorite) 1 else 0,
                        COLUMN_DATE to DateFormat.getInstance().format(date))
            }
            return tag
        }
        return null
    }

    fun addSubscription(name: String, startID: Int) {
        val sub = Subscription(name, startID, startID)
        if (subs.find { it.name == name } == null) {
            subs += sub
            use {
                insert(TABLE_SUBSCRIPTION,
                        COLUMN_NAME to name,
                        COLUMN_SUBSCRIBED_SINCE to startID,
                        COLUMN_SUBSCRIBED_SINCE to startID)
            }
        }
    }

    fun removeTag(name: String) {
        val tag = tags.find { it.name == name }
        if (tag != null) {
            tags.remove(tag)
            use {
                delete(TABLE_TAGS, whereClause = "$COLUMN_NAME = {name}", args = *arrayOf("name" to name))
            }
        }
    }

    fun removeSubscription(name: String) {
        val sub = subs.find { it.name == name }
        if (sub != null) {
            subs.remove(sub)
            use {
                delete(TABLE_SUBSCRIPTION, whereClause = "$COLUMN_NAME = {name}", args = *arrayOf("name" to name))
            }
        }
    }

    fun changeTag(changedTag: Tag) {
        val tag = tags.find { it.name == changedTag.name }
        if (tag != null) {
            tag.isFavorite = changedTag.isFavorite
            use {
                update(TABLE_TAGS,
                        COLUMN_IS_FAVORITE to if (changedTag.isFavorite) 1 else 0)
                        .whereArgs("$COLUMN_NAME = {name}", "name" to changedTag.name).exec()
            }
        }
    }

    fun changeSubscription(changedSub: Subscription) {
        val sub = subs.find { it.name == changedSub.name }
        if (sub != null) {
            sub.currentID = changedSub.currentID
            use {
                update(TABLE_SUBSCRIPTION,
                        COLUMN_SUBSCRIBED_STATUS to changedSub.currentID)
                        .whereArgs("$COLUMN_NAME = {name}", "name" to changedSub.name).exec()
            }
        }
    }

    fun getSubscription(name: String) = getSubscriptions().find { it.name == name }
    fun getTag(name: String) = getTags().find { it.name == name }

    fun getTags(): List<Tag> {
        if (tags.isNotEmpty()) return tags
        else return use {
            val t = ArrayList<Tag>()
            select(TABLE_TAGS, COLUMN_NAME, COLUMN_IS_FAVORITE, COLUMN_DATE).parseList(object : MapRowParser<ArrayList<Tag>> {
                override fun parseRow(columns: Map<String, Any?>): ArrayList<Tag> {
                    val name = columns[COLUMN_NAME].toString()
                    val isFavorite = (columns[COLUMN_IS_FAVORITE] as Long).toInt()
                    val date = DateFormat.getInstance().parse(columns[COLUMN_DATE].toString())
                    val type = columns[COLUMN_TYPE].toString()

                    t += Tag(name, type, isFavorite == 1, date)
                    return t
                }
            })
            t
        }
    }

    fun getSubscriptions(): List<Subscription> {
        if (subs.isNotEmpty()) return subs
        else return use {
            val s = ArrayList<Subscription>()
            select(TABLE_SUBSCRIPTION, COLUMN_NAME, COLUMN_SUBSCRIBED_SINCE, COLUMN_SUBSCRIBED_STATUS).parseList(object : MapRowParser<ArrayList<Subscription>> {
                override fun parseRow(columns: Map<String, Any?>): ArrayList<Subscription> {
                    val name = columns[COLUMN_NAME].toString()
                    val startID = (columns[COLUMN_SUBSCRIBED_SINCE] as Long).toInt()
                    val currentID = (columns[COLUMN_SUBSCRIBED_STATUS] as Long).toInt()
                    s += Subscription(name, startID, currentID)
                    return s
                }
            })
            s
        }
    }

    private var _r18: Boolean? = null
        set(value) {
            field = value
            with(prefs.edit()) {
                putBoolean("r18", value!!)
                apply()
            }
        }
    var r18: Boolean
        get() {
            if (_r18 == null) _r18 = prefs.getBoolean("r18", false)
            return _r18!!
        }
        set(value) {
            _r18 = value
        }


    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(TABLE_TAGS, true,
                COLUMN_NAME to TEXT + PRIMARY_KEY + UNIQUE,
                COLUMN_IS_FAVORITE to INTEGER,
                COLUMN_DATE to TEXT)
        db.createTable(TABLE_SUBSCRIPTION, true,
                COLUMN_NAME to TEXT + PRIMARY_KEY + UNIQUE,
                COLUMN_SUBSCRIBED_SINCE to INTEGER,
                COLUMN_SUBSCRIBED_STATUS to INTEGER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

}

val Context.database: Database
    get() = Database.getInstance(this)
