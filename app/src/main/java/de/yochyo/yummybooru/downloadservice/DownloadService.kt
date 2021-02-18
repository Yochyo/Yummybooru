package de.yochyo.yummybooru.downloadservice

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.manager.IManager
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.utils.app.App
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.general.getDownloadPathAndId
import de.yochyo.yummybooru.utils.network.CacheableDownloader
import kotlinx.coroutines.*
import java.util.*

private class DownloadPost(val tags: String, val post: Post, val server: Server)
class DownloadService : Service() {
    private val downloader = CacheableDownloader(preferences.parallelBackgroundDownloads)

    var job: Job? = null
    lateinit var notificationManager: NotificationManagerCompat
    lateinit var notificationBuilder: NotificationCompat.Builder

    companion object {
        const val NOTIFICATION_ID = 1
        private val util = ServiceDownloadUtil<DownloadPost>()
        suspend fun startService(context: Context, tags: String, posts: List<Post>, server: Server) {
            util += posts.map { DownloadPost(tags, it, server) }
            context.startService(Intent(context, DownloadService::class.java))
        }

        suspend fun startService(context: Context, manager: IManager, server: Server) = startService(context, manager.toString(), ArrayList(manager.posts), server)
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat.Builder(this, App.CHANNEL_ID).setSmallIcon(R.drawable.notification_icon).setContentTitle(getString(R.string.downloading))
            .setOngoing(true).setLocalOnly(true).setProgress(100, 0, false).setNotificationSilent()
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        job = GlobalScope.launch(Dispatchers.IO) {
            downloadPosts()
            while (downloader.dl.activeCoroutines > 0 && isActive) {
                delay(2000)
                downloadPosts()
            }

            stopSelf()
        }
    }

    suspend fun downloadPosts() {
        var next = util.popOrNull()
        while (next != null) {
            val finalNext = next
            val url = getDownloadPathAndId(this@DownloadService, next.post).first
            downloader.download(url, {
                if (it != null) {
                    FileUtils.writeFile(this@DownloadService, finalNext.post, it, finalNext.server)
                    withContext(Dispatchers.Main) {
                        util.announceFinishedDownload()
                        updateNotification(finalNext)
                    }
                }
            }, next.server.headers)
            next = util.popOrNull()
        }
    }

    private suspend fun updateNotification(post: DownloadPost) {
        withContext(Dispatchers.Main) {
            notificationBuilder.setContentTitle(getString(R.string.downloading_part_n_of_m, util.downloaded, util.totalSize))
            notificationBuilder.setContentText(post.tags)
            notificationBuilder.setProgress(util.totalSize, util.downloaded, false)
            notificationManager.notify(1, notificationBuilder.build())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
        runBlocking { util.clear() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null
}