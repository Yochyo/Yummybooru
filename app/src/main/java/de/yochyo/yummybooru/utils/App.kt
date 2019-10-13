package de.yochyo.yummybooru.utils

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.api.DanbooruApi
import de.yochyo.yummybooru.api.api.MoebooruApi
import de.yochyo.yummybooru.events.events.*
import de.yochyo.yummybooru.events.listeners.*

class App : Application() {
    companion object {
        const val CHANNEL_ID = "serviceChannel"
    }

    override fun onCreate() {
        Thread.setDefaultUncaughtExceptionHandler(ThreadExceptionHandler())
        super.onCreate()
        initListeners()
        Api.addApi(DanbooruApi(""))
        Api.addApi(MoebooruApi(""))
        createNotificationChannel()
    }

    private fun initListeners() {
        AddTagEvent.registerListener(DisplayToastAddTagListener())
        AddSubEvent.registerListener(DisplayToastAddSubListener())
        AddServerEvent.registerListener(DisplayToastAddServerListener())
        DeleteServerEvent.registerListener(DisplayToastDeleteServerListener())
        DeleteSubEvent.registerListener(DisplayToastDeleteSubListener())
        DeleteTagEvent.registerListener(DisplayToastDeleteTagListener())
        ChangeSubEvent.registerListener(DisplayToastFavoriteSubListener())
        ChangeTagEvent.registerListener(DisplayToastFavoriteTagListener())
        DeleteTagEvent.registerListener(RemoveSelectedTagsInMainactivityListener())
        ChangeServerEvent.registerListener(DisplayToastChangeServerEvent())
        SelectServerEvent.registerListener(DisplayToastSelectServerListener())
        SelectServerEvent.registerListener(ClearSelectedTagsInMainactivityListener())
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