package de.yochyo.yummybooru.downloadservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.yochyo.booruapi.api.Post
import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventcollection.observable.Observable
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Resource2
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.app.App
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.general.FileWriteResult
import de.yochyo.yummybooru.utils.network.CacheableDownloader
import kotlinx.coroutines.*
import java.util.*

private typealias Download = Triple<String, String, suspend (e: Resource2?) -> Unit> //url, id, callback

class InAppDownloadService : Service() {
    private val downloader = CacheableDownloader(db.parallelBackgroundDownloads)

    private val onAddDownloadListener = Listener<OnChangeObjectEvent<Observable<Int>, Int>> {
        if (this::notificationManager.isInitialized && this::notificationBuilder.isInitialized)
            updateNotification(it.new.value)
    }

    var job: Job? = null
    lateinit var notificationManager: NotificationManagerCompat
    lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        private var count = Observable(0)
        private val downloads = EventCollection(LinkedList<Download>())
        fun startService(context: Context, url: String, id: String, callback: suspend (e: Resource2?) -> Unit) {
            downloads += Download(url, id, callback)
            count.value++
            context.startService(Intent(context, InAppDownloadService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        count.onChange.registerListener(onAddDownloadListener)
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder =
            NotificationCompat.Builder(this, App.CHANNEL_ID).setSmallIcon(R.drawable.notification_icon).setContentTitle("Downloading ... (Queue size: ${downloads.size})")
                .setOngoing(true).setLocalOnly(true)
        startForeground(2, notificationBuilder.build())
        job = GlobalScope.launch(Dispatchers.IO) {
            do {
                while (downloads.isNotEmpty()) {
                    val dl = downloads.first().apply { downloads.remove(this) }
                    downloader.download(dl.first, {
                        dl.third(it)
                        onFinishDownload(dl)
                        count.value--
                    })
                }
                delay(5000)
            } while (downloader.dl.activeCoroutines > 0)

            stopSelf()
        }
    }

    private fun updateNotification(value: Int) {
        notificationBuilder.setContentTitle("Downloading ... (Queue size: ${value})")
        notificationManager.notify(2, notificationBuilder.build())
    }

    private fun onFinishDownload(dl: Download) {
        if (!db.hideDownloadToast)
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(
                    this@InAppDownloadService,
                    getString(R.string.download_post_with_id, getIdFromMergedId(dl.second).toInt()), Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        count.onChange.removeListener(onAddDownloadListener)
        job?.cancel()
    }

    override fun onBind(intent: Intent): IBinder? = null
}

fun saveDownload(context: Context, url: String, id: String, post: Post) = InAppDownloadService.startService(context, url, id) {
    if (it != null) {
        if (FileUtils.writeFile(context, post, it, context.db.currentServer) == FileWriteResult.FAILED)
            withContext(Dispatchers.Main) { Toast.makeText(context, "Saving ${getIdFromMergedId(id)} failed", Toast.LENGTH_SHORT).show() }
    } else withContext(Dispatchers.Main) { Toast.makeText(context, "Failed downloading ${getIdFromMergedId(id)}", Toast.LENGTH_SHORT).show() }
}

private fun getIdFromMergedId(id: String): String = id.takeWhile { it in '0'..'9' }