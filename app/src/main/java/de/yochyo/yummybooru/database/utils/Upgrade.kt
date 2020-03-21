package de.yochyo.yummybooru.database.utils

import android.database.sqlite.SQLiteDatabase
import de.yochyo.yummybooru.database.updates.Upgrade2
import de.yochyo.yummybooru.utils.general.Logger

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
            Logger.log("upgrading")
            for(version in oldVersion until newVersion){
                Logger.log("$version")
                val upgrade = upgrades.find { it.oldVersion == version }
                if (upgrade == null) throw Exception("Couldn't upgrade database")
                else upgrade.upgrade(sql)
            }
        }
    }


    abstract fun upgrade(sql: SQLiteDatabase)
}