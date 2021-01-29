package de.yochyo.yummybooru.utils.general

import android.content.Context
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object SettingsUtils {
    fun updateTagComparator(context: Context) {
        GlobalScope.launch { context.db.reloadDB() }
    }
}