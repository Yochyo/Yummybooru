package de.yochyo.yummybooru.downloadservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.downloads.Downloader
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.SafeFileEvent
import de.yochyo.yummybooru.utils.App
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
        private val downloadPosts = LinkedList<Posts>()

        fun startService(context: Context, tags: String, posts: List<Post>){
            downloadPosts += Posts(tags, posts)
            context.startService(Intent(context, DownloadService::class.java))
        }
        fun startService(context: Context, manager: Manager) = startService(context, manager.tags.toTagString(), ArrayList(manager.posts))
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
                val bitmap = if (db.downloadOriginal) Downloader.download(p.fileSampleURL)
                else Downloader.download(p.fileURL)
                if (bitmap != null) FileUtils.writeFile(this@DownloadService, p, bitmap, SafeFileEvent.SILENT)
                p = getNextElement()
            }
            stopSelf()
        }
    }

    private suspend fun getNextElement(): Post? {
        if (downloadPosts.isNotEmpty()) {
            val posts = downloadPosts[0]
            return if (posts.posts.size > position) {
                val post = posts.posts[position]
                withContext(Dispatchers.Main) {
                    notificationBuilder.setContentTitle("Downloading $position/${posts.posts.size}")
                    notificationBuilder.setContentText(posts.tags)
                    notificationBuilder.setProgress(posts.posts.size, position, false)
                    notificationManager.notify(1, notificationBuilder.build())
                }
                ++position
                post
            } else {
                position = 0
                downloadPosts.removeAt(0)
                getNextElement()
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

private class Posts(val tags: String, val posts: List<Post>)
