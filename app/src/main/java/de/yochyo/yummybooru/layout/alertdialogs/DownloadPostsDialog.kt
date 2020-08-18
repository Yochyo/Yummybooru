package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import de.yochyo.booruapi.manager.IManager
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.DownloadService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DownloadPostsDialog(context: Context, manager: IManager) {
    init {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.download_all))
        builder.setMessage(context.getString(R.string.check_storage_space))
        builder.setPositiveButton(context.getString(R.string.positive_button_name)) { _, _ ->
            GlobalScope.launch {
                val server = context.db.currentServer
                DownloadService.startService(context, manager, server)
                do {
                    val page = manager.downloadNextPage()
                    if (page == null || page.isEmpty()) break
                    else DownloadService.startService(context, manager.toString(), page, server)
                } while (true)
            }
        }
        builder.show()
    }
}