package de.yochyo.yummybooru.database.dao

import org.jetbrains.anko.db.ManagedSQLiteOpenHelper

abstract class Dao(val database: ManagedSQLiteOpenHelper) {
    abstract fun createTable()
}