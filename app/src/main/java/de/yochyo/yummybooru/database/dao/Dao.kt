package de.yochyo.yummybooru.database.dao

import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.ManagedSQLiteOpenHelper

abstract class Dao(val database: ManagedSQLiteOpenHelper) {
    abstract fun createTable(database: SQLiteDatabase)
}