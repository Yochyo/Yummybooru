package de.yochyo.yummybooru.downloadservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.yochyo.downloader.RegulatingDownloader
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.entities.Resource
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.SafeFileEvent
import de.yochyo.yummybooru.utils.Manager
import de.yochyo.yummybooru.utils.app.App
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.general.toTagString
import kotlinx.coroutines.*
import java.io.InputStream
import java.util.*

class DownloadService : Service() {
    private val downloader = object : RegulatingDownloader<Resource>(1) {
        override fun toResource(inputStream: InputStream, context: Any): Resource {
            return Resource(inputStream.readBytes(), context as Int)
        }
    }

    var job: Job? = null
    lateinit var notificationManager: NotificationManagerCompat
    lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        private var position = 0
        private val downloadPosts = LinkedList<Posts>()

        fun startService(context: Context, tags: String, posts: List<Post>, server: Server) {
            downloadPosts += Posts(tags, posts, server)
            context.startService(Intent(context, DownloadService::class.java))
        }

        fun startService(context: Context, manager: Manager, server: Server) = startService(context, manager.tags.toTagString(), ArrayList(manager.posts), server)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, App.CHANNEL_ID).setSmallIcon(R.drawable.notification_icon).setContentTitle("Downloading")
                .setOngoing(true).setLocalOnly(true).setProgress(100, 0, false)

        startForeground(1, notificationBuilder.build())
        job = GlobalScope.launch(Dispatchers.IO) {
            var pair = getNextElement()
            while (pair != null && isActive) {
                val url = if (db.downloadOriginal) pair.first.fileURL else pair.first.fileSampleURL
                val image = downloader.downloadSync(url, this)
                if (image != null) FileUtils.writeFile(this@DownloadService, pair.first, image, pair.second, SafeFileEvent.SILENT)
                pair = getNextElement()
            }
            stopSelf()
        }
    }

    private suspend fun getNextElement(): Pair<Post, Server>? {
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
                Pair(post, posts.server)
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

private class Posts(val tags: String, val posts: List<Post>, val server: Server)
