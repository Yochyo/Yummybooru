package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.downloadservice.DownloadService
import de.yochyo.yummybooru.utils.Manager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class DownloadPostsAlertdialog(context: Context, manager: Manager) {
    init {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Download all (attention)")
        builder.setMessage("Make sure you have enough storage space")
        builder.setPositiveButton("Download") { _, _ ->
            GlobalScope.launch {
                while (manager.loadNextPage(context)?.isNotEmpty() == true) {

                }
                DownloadService.startService(context, manager, Server.getCurrentServer(context))
            }
        }
        builder.show()
    }
}