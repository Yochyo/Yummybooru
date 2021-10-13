package de.yochyo.yummybooru.downloadservice

import android.app.Notification
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import de.yochyo.booruapi.api.Post
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.utils.app.App
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.general.FileWriteResult
import de.yochyo.yummybooru.utils.network.CacheableDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class Data(val url: String, val headers: Map<String, String>, val post: Post, val server: Server)
class InAppDownloadWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val downloader = CacheableDownloader(1)

    companion object {
        const val NOTIFICATION_ID = 1

        private val util = ServiceDownloadUtil<Data>()
        suspend fun startService(context: Context, data: Data) {
            util += data
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequest.Builder(InAppDownloadWorker::class.java).build())
        }
    }

    override suspend fun doWork(): Result {
        withContext(Dispatchers.IO) {
            setForeground(createForegroundInfo())
            repeat(applicationContext.preferences.parallelBackgroundDownloads) {
                var next = util.popOrNull()
                while (next != null) {
                    download(next)
                    next = util.popOrNull()
                }
            }
        }
        return Result.success()
    }

    private suspend fun download(dl: Data) {
        val it = downloader.downloadSync(dl.url, dl.headers)
        if (it != null) {
            if (FileUtils.writeFile(applicationContext, dl.post, it, dl.server) == FileWriteResult.FAILED)
                withContext(Dispatchers.Main) { Toast.makeText(applicationContext, "Saving ${dl.post.id} failed", Toast.LENGTH_SHORT).show() }
        } else withContext(Dispatchers.Main) { Toast.makeText(applicationContext, "Failed downloading ${dl.post.id}", Toast.LENGTH_SHORT).show() }
        setForeground(createForegroundInfo())
        // Downloads a file and updates bytes read
        // Calls setForegroundAsync(createForegroundInfo(myProgress))
        // periodically when it needs to update the ongoing Notification.
    }

    private fun createForegroundInfo(): ForegroundInfo {
        // Build a notification using bytesRead and contentLength
        val context = applicationContext
        val cancel = context.getString(R.string.cancel)
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(context)
            .createCancelPendingIntent(getId())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification: Notification = NotificationCompat.Builder(context, App.CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle("Downloading ...")
            .setOngoing(true).setLocalOnly(true).setSilent(true)
            .addAction(R.drawable.clear, cancel, intent)
            .build()
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel() {
        // Create a Notification channel
    }
}

fun saveDownload(context: Context, url: String, id: String, server: Server, post: Post) =
    GlobalScope.launch { InAppDownloadWorker.startService(context, Data(url, server.headers, post, server)) }

