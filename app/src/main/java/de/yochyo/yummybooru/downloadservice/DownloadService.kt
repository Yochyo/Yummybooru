package de.yochyo.yummybooru.downloadservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.downloads.Downloader
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.SafeFileEvent
import de.yochyo.yummybooru.utils.FileUtils
import de.yochyo.yummybooru.utils.toTagString
import kotlinx.coroutines.*
import java.util.*

class DownloadService : Service() {
    var job: Job? = null
    lateinit var notificationManager: NotificationManagerCompat
    lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        private var position = 0
        private val downloadPosts = LinkedList<Manager>()
        fun startService(context: Context, manager: Manager) {
            downloadPosts += manager
            context.startService(Intent(context, DownloadService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, App.CHANNEL_ID).setSmallIcon(R.mipmap.ybooru_icon).setContentTitle("Downloading")
                .setOngoing(true).setLocalOnly(true).setProgress(100, 0, false)

        startForeground(1, notificationBuilder.build())
        job = GlobalScope.launch(Dispatchers.IO) {
            var p: Post? = getNextElement()
            while (p != null && isActive) {
                val bitmap: Bitmap?
                bitmap = if (db.downloadOriginal) Downloader.download(p.fileSampleURL)
                else Downloader.download(p.fileURL)
                if (bitmap != null) FileUtils.writeFile(this@DownloadService, p, bitmap, SafeFileEvent.SILENT)
                p = getNextElement()
            }
            stopSelf()
        }
    }

    private suspend fun getNextElement(): Post? {
        if (downloadPosts.isNotEmpty()) {
            val manager = downloadPosts[0]
            val posts = manager.posts.value
            if (posts != null) {
                if (posts.size > position) {
                    val post = posts[position]
                    withContext(Dispatchers.Main) {
                        notificationBuilder.setContentTitle("Downloading $position/${posts.size}")
                        notificationBuilder.setContentText(manager.tags.toTagString())
                        notificationBuilder.setProgress(posts.size, position, false)
                        notificationManager.notify(1, notificationBuilder.build())
                    }
                    ++position
                    return post
                } else {
                    position = 0
                    downloadPosts.removeAt(0)
                    return getNextElement()
                }
            }
        }
        return null
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

private class BackgroundPostDownload(val post: Post, val tags: String)
