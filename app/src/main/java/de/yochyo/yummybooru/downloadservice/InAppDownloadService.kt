package de.yochyo.yummybooru.downloadservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Resource2
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.app.App
import de.yochyo.yummybooru.utils.network.CacheableDownloader
import kotlinx.coroutines.*
import java.util.*

private typealias Download = Triple<String, String, suspend (e: Resource2) -> Unit> //url, id, callback

class InAppDownloadService : Service() {
    private val downloader = CacheableDownloader(db.parallelBackgroundDownloads)

    var job: Job? = null
    lateinit var notificationManager: NotificationManagerCompat
    lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        private val downloads = LinkedList<Download>()
        fun startService(context: Context, url: String, id: String, callback: suspend (e: Resource2) -> Unit) {
            downloads += Download(url, id, callback)
            context.startService(Intent(context, InAppDownloadService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, App.CHANNEL_ID).setSmallIcon(R.drawable.notification_icon).setContentTitle("Downloading ...")
            .setOngoing(true).setLocalOnly(true)
        startForeground(2, notificationBuilder.build())
        job = GlobalScope.launch(Dispatchers.IO) {
            do {
                while (downloads.isNotEmpty()) {
                    val dl = downloads.removeFirst()
                    downloader.download(dl.first, {
                        dl.third(it)
                        onFinishDownload(dl)
                    })
                }
                delay(5000)
            } while (downloader.dl.activeCoroutines > 0)

            stopSelf()
        }
    }

    private fun onFinishDownload(dl: Download) {
        if (!db.hideDownloadToast)
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    this@InAppDownloadService,
                    getString(R.string.download_post_with_id, dl.second.takeWhile { it in '0'..'9' }.toInt()), Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = null
}