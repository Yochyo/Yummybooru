package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import de.yochyo.booruapi.manager.IManager
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.DownloadService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DownloadPostsAlertdialog(context: Context, manager: IManager) {
    init {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Download all")
        builder.setMessage("Make sure you have enough storage space")
        builder.setPositiveButton("Download") { _, _ ->
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