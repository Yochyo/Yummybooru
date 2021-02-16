package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ProgressBar
import android.widget.TextView
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.R
import kotlinx.coroutines.*

class ProgressDialog(val observable: EventHandler<OnChangeObjectEvent<Int, Int>>/*Newprogress, TotalProgress*/) {
    var title: String = "Progress"
    private lateinit var dialog: AlertDialog
    private lateinit var progressBar: ProgressBar
    private lateinit var progressTextView: TextView
    private lateinit var progressMaxTextView: TextView

    private lateinit var updateJob: Job


    private var progress = 0
    private var total = 0

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setCancelable(false)

        val view = LayoutInflater.from(context).inflate(R.layout.restore_backup_layout, null, false)
        progressBar = view.findViewById(R.id.restore_progress_bar)
        progressTextView = view.findViewById(R.id.restore_backup_current_progress_text)
        progressMaxTextView = view.findViewById(R.id.restore_backup_total_progress_text)

        builder.setView(view)

        observable.registerListener { progress = it.new; total = it.arg }
        updateJob = GlobalScope.launch(Dispatchers.Main) {
            while (isActive) {
                setProgress(progress, total)
                delay(100)
            }
        }
        this.dialog = builder.show()
    }

    private fun setProgress(progress: Int, total: Int) {
        progressTextView.text = progress.toString()
        progressMaxTextView.text = total.toString()
        progressBar.progress = progress
        progressBar.max = total
    }

    fun stop() {
        dialog.dismiss()
    }
}