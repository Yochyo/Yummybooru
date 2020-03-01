package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.DownloadService
import de.yochyo.yummybooru.utils.manager.IManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DownloadPostsAlertdialog(context: Context, manager: IManager) {
    init {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Download all")
        builder.setMessage("Make sure you have enough storage space")
        builder.setPositiveButton("Download") { _, _ ->
            GlobalScope.launch {
                val server = Server.getCurrentServer(context)
                DownloadService.startService(context, manager, server)
                do {
                    val page = manager.downloadNextPage(context, context.db.limit)
                    if (page == null || page.isEmpty()) break
                    else DownloadService.startService(context, manager.toString(), page, server)
                } while (true)
            }
        }
        builder.show()
    }
}