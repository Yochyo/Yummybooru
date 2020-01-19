package de.yochyo.yummybooru.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import de.yochyo.yummybooru.database.dao.*
import de.yochyo.yummybooru.database.utils.Upgrade
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper

class NewDatabase(context: Context): ManagedSQLiteOpenHelper(context, "db", null, 2){
    override fun onCreate(db: SQLiteDatabase?) {
        tagDao.createTable()
        subDao.createTable()
        serverDao.createTable()
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Upgrade.upgradeFromTo(db, oldVersion, newVersion)
    }

    private val tagDao = TagDao(this)
    private val subDao = SubDao(this)
    private val serverDao = ServerDao(this)
}