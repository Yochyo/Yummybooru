package de.yochyo.yummybooru.utils.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import de.yochyo.yummybooru.events.events.*
import de.yochyo.yummybooru.events.listeners.*
import de.yochyo.yummybooru.utils.general.ThreadExceptionHandler

class App : Application() {
    companion object {
        const val CHANNEL_ID = "serviceChannel"
    }

    override fun onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(ThreadExceptionHandler())
        super.onCreate()
        initListeners()
        createNotificationChannel()
    }

    private fun initListeners() {
        SelectServerEvent.registerListener(DisplayToastSelectServerListener())
        SafeFileEvent.registerListener(DisplayToastDownloadFileListener())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Service Channel", NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}