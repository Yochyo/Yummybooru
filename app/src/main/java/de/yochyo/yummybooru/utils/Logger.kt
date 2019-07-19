package de.yochyo.yummybooru.utils

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.lang.StringBuilder

object Logger {
    private val directory = File("$configPath/logs/")
    init{
        directory.mkdirs()
    }

    fun log(message: String) {
        val files = directory.listFiles().sorted()
        if (files.size > 200) //keep the amount of logs to a minimum
            for (i in 0..files.size / 2)
                files[i].delete()

        val logFile = File(directory, "logcat ${System.currentTimeMillis()}.txt")
        logFile.createNewFile()
        logFile.writeText(message)
    }
}

class ThreadExceptionHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (e != null) {
            val errors = StringWriter()
            e.printStackTrace(PrintWriter(errors))
            Logger.log(errors.toString())
        }
    }
}