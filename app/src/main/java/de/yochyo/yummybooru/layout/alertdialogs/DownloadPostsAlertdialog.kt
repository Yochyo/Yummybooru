package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.downloadservice.DownloadService

class DownloadPostsAlertdialog(context: Context, manager: Manager) {
    init {
        val builder = AlertDialog.Builder(context)
        builder.setItems(arrayOf("Download all", "Download visible")) { _, pos ->
            when (pos) {
                0 -> Toast.makeText(context, "Not implemented yet because of security reasons", Toast.LENGTH_SHORT).show()
                1 -> {
                    DownloadService.startService(context, manager)
                    Toast.makeText(context, "Download all visible", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.show()
    }
}