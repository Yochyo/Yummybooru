package de.yochyo.yummybooru.utils

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object Logger {
    private val directory = File("$configPath/logs/")

    init {
        directory.mkdirs()
    }

    fun log(message: String) {
        val files = directory.listFiles().sorted()
        for (i in 0..files.size / 2)
            files[i].delete()

        val logFile = File(directory, "logcat ${System.currentTimeMillis()}.txt")
        logFile.createNewFile()
        logFile.writeText(message)
    }

    fun log(e: Throwable, info: String = "") {
        val errors = StringWriter()
        e.printStackTrace(PrintWriter(errors))
        log("$info\n$errors")
    }
}

class ThreadExceptionHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (e != null)
            Logger.log(e)
        exitProcess(10)
    }
}