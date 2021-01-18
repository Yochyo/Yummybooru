package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import de.yochyo.yummybooru.R

class RestoreBackupDailog(val progressBarSize: Int) {
    var title: String = "Restoring backup"
    private lateinit var progressBar: ProgressBar
    private lateinit var progressTextView: TextView
    private lateinit var dialog: AlertDialog

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setCancelable(false)

        val view = LayoutInflater.from(context).inflate(R.layout.restore_backup_layout, null, false)
        progressBar = view.findViewById(R.id.restore_progress_bar)
        progressTextView = view.findViewById(R.id.restore_backup_current_progress_text)

        view.findViewById<TextView>(R.id.restore_backup_total_progress_text).text = progressBarSize.toString()
        progressBar.max = progressBarSize
        builder.setView(view)

        this.dialog = builder.show()
    }

    var progress
        get() = progressBar.progress
        set(value) {
            progressBar.progress = value
            progressTextView.text = value.toString()
        }

    fun stop() {
        dialog.dismiss()
    }
}