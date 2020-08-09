package de.yochyo.yummybooru.downloadservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.yochyo.booruapi.objects.Post
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Resource
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.app.App
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.general.currentServer
import de.yochyo.yummybooru.utils.network.CacheableDownloader
import kotlinx.coroutines.*
import java.util.*

private typealias Download = Triple<String, String, suspend (e: Resource) -> Unit>
class InAppDownloadService : Service() {
    private val downloader = CacheableDownloader(db.parallelBackgroundDownloads)

    var job: Job? = null
    lateinit var notificationManager: NotificationManagerCompat
    lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        private val downloads = LinkedList<Download>()
        fun startService(context: Context, url: String, id: String, callback: suspend (e: Resource) -> Unit) {
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
            do{
                while(downloads.isNotEmpty()){
                    val dl = downloads.removeFirst()
                    downloader.download(this@InAppDownloadService, dl.first, dl.second, dl.third)
                }
                delay(5000)
            } while (downloader.dl.activeCoroutines > 0)

            stopSelf()
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

fun saveDownload(context: Context, url: String, id: String, post: Post) = InAppDownloadService.startService(context, url, id) {
    FileUtils.writeFile(context, post, it, context.currentServer)
}