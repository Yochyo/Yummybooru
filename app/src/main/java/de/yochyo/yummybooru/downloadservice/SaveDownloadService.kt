package de.yochyo.yummybooru.downloadservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.entities.Resource
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.utils.app.App
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.network.CacheableDownloader
import kotlinx.coroutines.*

class SingleDownloadService : Service() {

    var job: Job? = null
    lateinit var notificationManager: NotificationManagerCompat
    lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        private val downloader = CacheableDownloader(2)
        fun startService(context: Context, url: String, id: String, callback: suspend (e: Resource) -> Unit) {
            downloader.download(context, url, id, callback)
            context.startService(Intent(context, SingleDownloadService::class.java))
        }

    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, App.CHANNEL_ID).setSmallIcon(R.drawable.notification_icon).setContentTitle("Downloading ...")
                .setOngoing(true).setLocalOnly(true)
        startForeground(2, notificationBuilder.build())
        job = GlobalScope.launch(Dispatchers.IO) {
            while (downloader.dl.activeCoroutines > 0) {
                delay(5000)
            }
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

fun saveDownload(context: Context, url: String, id: String, post: Post) = SingleDownloadService.startService(context, url, id) {
    FileUtils.writeFile(context, post, it, Server.getCurrentServer(context))
}