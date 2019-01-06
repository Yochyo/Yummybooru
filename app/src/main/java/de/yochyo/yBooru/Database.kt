package de.yochyo.yBooru

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.*

class Database(context: Context) : ManagedSQLiteOpenHelper(context, "database", null, 1) {
    private val TABLE_TAGS = "tags"
    private val COLUMN_NAME = "name"
    private val COLUMN_IS_FAVORITE = "isFavorite"
    private val COLUMN_IS_SUBSCRIBED = "isSubscribed"

    companion object {
        private var _instance: Database? = null
        fun getInstance(context: Context): Database {
            return if (_instance != null) _instance!!
            else Database(context).apply { _instance = this }
        }
    }

    private val tags = ArrayList<Tag>()

    init {
        tags += getTags()
    }


    fun addTag(tag: Tag) {
        if (tags.find { it.name == tag.name } == null) {
            tags += tag
            use {
                insert(TABLE_TAGS,
                        COLUMN_NAME to tag.name,
                        COLUMN_IS_FAVORITE to if (tag.isFavorite) 1 else 0,
                        COLUMN_IS_SUBSCRIBED to if (tag.isSubscribed) 1 else 0)
            }
        }
    }

    fun removeTag(name: String) {
        val tag = tags.find { it.name == name }
        if (tag != null) {
            tags.remove(tag)
            use {
                delete(TABLE_TAGS, whereClause = "id = {name}", args = *arrayOf("name" to name))
            }
        }
    }

    fun changeTag(changedTag: Tag) {
        val tag = tags.find { it.name == changedTag.name }
        if (tag != null) {
            tag.isFavorite = changedTag.isFavorite
            tag.isSubscribed = changedTag.isSubscribed
            use {
                update(TABLE_TAGS,
                        COLUMN_IS_FAVORITE to if (changedTag.isFavorite) 1 else 0,
                        COLUMN_IS_SUBSCRIBED to if (changedTag.isSubscribed) 1 else 0).whereArgs("$COLUMN_NAME = {name}", "name" to changedTag.name).exec()
            }
        }
    }

    fun getTag(name: String): Tag? {
        return getTags().find { it.name == name }
    }

    fun getTags(): List<Tag> {
        if (tags.isNotEmpty()) return tags
        else return use {
            val t = ArrayList<Tag>()
            select(TABLE_TAGS, COLUMN_IS_FAVORITE, COLUMN_NAME).parseList(object : MapRowParser<ArrayList<Tag>> {
                override fun parseRow(columns: Map<String, Any?>): ArrayList<Tag> {
                    columns[COLUMN_NAME]
                    val name = columns[COLUMN_NAME].toString()
                    val isFavorite = columns[COLUMN_IS_FAVORITE] as Int
                    val isSubscribed = columns[COLUMN_IS_SUBSCRIBED] as Int
                    t += Tag(name, isFavorite == 1, isSubscribed == 1)
                    return t
                }
            })
            t
        }

    }


    override fun onCreate(db: SQLiteDatabase) {
        db.createTable(TABLE_TAGS, true,
                COLUMN_NAME to TEXT + PRIMARY_KEY + UNIQUE,
                COLUMN_IS_FAVORITE to INTEGER,
                COLUMN_IS_SUBSCRIBED to INTEGER)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

}

val Context.database: Database
    get() = Database.getInstance(this)

class Tag(val name: String, var isFavorite: Boolean, var isSubscribed: Boolean)