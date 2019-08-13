package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.downloadservice.DownloadService

class DownloadPostsAlertdialog(context: Context, manager: Manager) {
    init {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Download all (attention)")
        builder.setMessage("All! pictures will be downloaded, make sure you have enough storage space")
        Toast.makeText(context, "Not implemented yet because of security reasons", Toast.LENGTH_SHORT).show()
        builder.show()
    }
}