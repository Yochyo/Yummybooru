package de.yochyo.yummybooru.updater

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.utils.app.App
import de.yochyo.yummybooru.utils.general.Logger
import de.yochyo.yummybooru.utils.general.configPath
import de.yochyo.yummybooru.utils.network.DownloadUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class AutoUpdater {
    private val saveDirectory = File("$configPath/updates").apply { mkdirs() }

    fun autoUpdate(context: Context) {
        GlobalScope.launch {
            if (!isNewestVersion()) {
                val file = downloadNewestFile()
                if (file != null)
                    throwUpdateNotification(context, file)
            }
        }
    }

    private fun throwUpdateNotification(context: Context, file: File) {
        val builder = NotificationCompat.Builder(context, App.CHANNEL_ID).setContentTitle("Update now to version ${file.name.subSequence(0, file.name.lastIndex - 3)}")
                .setSmallIcon(R.drawable.notification_icon).setAutoCancel(true)
        val intent = installIntent(context, file)
        builder.setContentIntent(PendingIntent.getActivity(context, 2, intent, 0))
        NotificationManagerCompat.from(context).notify(2, builder.build())
    }

    private fun installUpdate(context: Context, file: File) {
        val intent = installIntent(context, file)
        context.startActivity(intent)
    }

    private fun installIntent(context: Context, file: File): Intent {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(FileProvider.getUriForFile(context, context.applicationContext.packageName + ".fileprovider", file), "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return intent
    }

    suspend fun downloadNewestFile(): File? { //path
        return withContext(Dispatchers.IO) {
            try {
                val file = File(saveDirectory, "${latestVersionName()}.apk")
                if (!file.exists()) {
                    val url = newestDownloadUrl()
                    if (url != null) {
                        val stream = DownloadUtils.getUrlInputStream(url)
                        if (stream != null) {
                            val byteArray = stream.readBytes()
                            stream.close()
                            file.createNewFile()
                            file.writeBytes(byteArray)
                        }
                    }
                }
                if (file.exists()) return@withContext file
            } catch (e: Exception) {
            }
            return@withContext null
        }
    }

    suspend fun newestDownloadUrl(): String? {
        val latest = latestVersionName()
        if (latest != null) return "https://github.com/Yochyo/Yummybooru/releases/download/$latest/$latest.apk"
        else return null
    }

    private suspend fun isNewestVersion() = BuildConfig.VERSION_NAME == latestVersionName()

    private suspend fun latestVersionName(): String? {
        val url = latestVersionUrl()
        try {
            return url?.split("/")?.last()
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.log(e, "Error when splitting $url")
        }
        return null
    }

    private suspend fun latestVersionUrl(): String? {
        return withContext(Dispatchers.IO) {
            try {
                val con = URL("https://github.com/Yochyo/Yummybooru/releases/latest").openConnection() as HttpsURLConnection
                con.instanceFollowRedirects = true
                con.connect()
                val `is` = con.inputStream
                val url = con.url.toString()
                `is`.close()
                return@withContext url
            } catch (e: Exception) {
            }
            return@withContext null
        }
    }
}