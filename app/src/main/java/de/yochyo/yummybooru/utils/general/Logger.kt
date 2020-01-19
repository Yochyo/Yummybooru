package de.yochyo.yummybooru.utils.general

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.system.exitProcess

object Logger {
    private val directory = File("$configPath/logs/")

    init {
        directory.mkdirs()
    }

    fun log(message: String, filePrefix: String = "logcat") {
        val logFile = File(directory, "$filePrefix ${System.currentTimeMillis()}.txt")
        logFile.createNewFile()
        logFile.writeText(message)
    }

    fun log(e: Throwable, info: String = "", filePrefix: String = "logcat") {
        val errors = StringWriter()
        e.printStackTrace(PrintWriter(errors))
        log("$info\n$errors", filePrefix)
    }
}

class ThreadExceptionHandler : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (e != null)
            Logger.log(e, filePrefix = "crash")
        exitProcess(10)
    }
}