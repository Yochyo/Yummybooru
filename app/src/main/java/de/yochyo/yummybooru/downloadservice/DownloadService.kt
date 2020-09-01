package de.yochyo.yummybooru.downloadservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.yochyo.booruapi.manager.IManager
import de.yochyo.booruapi.objects.Post
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.app.App
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.general.getDownloadPathAndId
import de.yochyo.yummybooru.utils.network.CacheableDownloader
import kotlinx.coroutines.*
import java.util.*

class DownloadService : Service() {
    private val downloader = CacheableDownloader(db.parallelBackgroundDownloads)

    var job: Job? = null
    lateinit var notificationManager: NotificationManagerCompat
    lateinit var notificationBuilder: NotificationCompat.Builder

    private var position = 0

    companion object {
        private var totalItemCount = 0
        private var currentItem = 0
        private val downloadPosts = LinkedList<Posts>()

        fun startService(context: Context, tags: String, posts: List<Post>, server: Server) {
            totalItemCount += posts.size
            downloadPosts += Posts(tags, posts, server)
            context.startService(Intent(context, DownloadService::class.java))
        }

        fun startService(context: Context, manager: IManager, server: Server) = startService(context, manager.toString(), ArrayList(manager.posts), server)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, App.CHANNEL_ID).setSmallIcon(R.drawable.notification_icon).setContentTitle(getString(R.string.downloading))
                .setOngoing(true).setLocalOnly(true).setProgress(100, 0, false)

        startForeground(1, notificationBuilder.build())
        job = GlobalScope.launch(Dispatchers.IO) {

            downloadPosts()
            while (downloader.dl.activeCoroutines > 0) {
                delay(2000)
                downloadPosts()
            }
            totalItemCount = 0
            currentItem = 0
            stopSelf()
        }
    }

    suspend fun downloadPosts() {
        var pair = getNextElement()
        while (pair != null) {
            val finalPair = pair
            val (url, _) = getDownloadPathAndId(this@DownloadService, pair.first)
            downloader.download(url) {
                FileUtils.writeFile(this@DownloadService, finalPair.first, it, finalPair.second.server)
                withContext(Dispatchers.Main) {
                    updateNotification(finalPair.second)
                }
            }
            pair = getNextElement()
        }
    }

    private fun getNextElement(): Pair<Post, Posts>? {
        if (downloadPosts.isNotEmpty()) {
            val posts = downloadPosts[0]
            return if (posts.posts.size > position) {
                val post = posts.posts[position]
                ++position
                Pair(post, posts)
            } else {
                position = 0
                downloadPosts.removeAt(0)
                getNextElement()
            }
        }
        return null
    }

    private suspend fun updateNotification(posts: Posts) {
        withContext(Dispatchers.Main) {
            notificationBuilder.setContentTitle(getString(R.string.downloading_part_n_of_m, currentItem, totalItemCount))
            notificationBuilder.setContentText(posts.tags)
            notificationBuilder.setProgress(totalItemCount, currentItem, false)
            notificationManager.notify(1, notificationBuilder.build())
        }
        ++currentItem
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
