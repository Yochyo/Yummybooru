package de.yochyo.yummybooru.database.upgrades

import android.database.sqlite.SQLiteDatabase

abstract class Upgrade(val oldVersion: Int) {
    companion object {
        private val upgrades = ArrayList<Upgrade>()

        init {
            upgrades += object : Upgrade(1) {
                override fun upgrade(sql: SQLiteDatabase) {
                }
            }
            upgrades += Upgrade2()
        }

        fun upgradeFromTo(sql: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            for(version in oldVersion until newVersion){
                val upgrade = upgrades.find { it.oldVersion == version }
                if (upgrade == null) throw Exception("Couldn't upgrade database")
                else upgrade.upgrade(sql)
            }
        }
    }


    abstract fun upgrade(sql: SQLiteDatabase)
}