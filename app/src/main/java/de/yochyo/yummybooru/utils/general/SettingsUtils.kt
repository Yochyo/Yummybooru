package de.yochyo.yummybooru.utils.general

import android.content.Context
import de.yochyo.yummybooru.database.db

object SettingsUtils {
    val a: String by lazy {
        Thread.sleep(1000)
        ""
    }

    fun updateTagComparator(context: Context) {
        context.db.clearTagCache()
    }
}