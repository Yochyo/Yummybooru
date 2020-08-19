package de.yochyo.yummybooru.utils.general

import de.yochyo.yummybooru.BuildConfig
import de.yochyo.yummybooru.utils.mail.Mail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.exitProcess


object Logger : de.yochyo.utils.Logger(configPath) {
    override fun log(message: String, filePrefix: String) {
        super.log(message, filePrefix)
        if (BuildConfig.BUILD_TYPE.equals("release", true)) {
            sendMails()
        }
    }

    fun sendMails() {
        for (file in directory.listFiles())
            sendMail(file)
    }

    private fun sendMail(file: File) {
        GlobalScope.launch(Dispatchers.IO) {
            val mail = Mail()
            try {
                if (mail.send(file.nameWithoutExtension, file.readText())) {
                    file.delete()
                }
            } catch (e: Exception) {
            }
        }

    }
}

class ThreadExceptionHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (e != null)
            Logger.log(e, filePrefix = "crash")
        exitProcess(10)
    }
}