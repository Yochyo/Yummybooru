package de.yochyo.yummybooru.database.utils

import android.database.sqlite.SQLiteDatabase

abstract class Upgrade(val oldVersion: Int) {
    companion object {
        private val upgrades = ArrayList<Upgrade>()

        init {
            upgrades += object : Upgrade(1) {
                override fun upgrade(sql: SQLiteDatabase) {
                }
            }
        }

        fun upgradeFromTo(sql: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            var currentVersion = oldVersion
            while (currentVersion < newVersion) {
                val upgrade = upgrades.find { oldVersion == currentVersion }
                if (upgrade == null) throw Exception("Couldn't upgrade database")
                else {
                    upgrade.upgrade(sql)
                }
                ++currentVersion
            }
        }
    }


    abstract fun upgrade(sql: SQLiteDatabase)
}