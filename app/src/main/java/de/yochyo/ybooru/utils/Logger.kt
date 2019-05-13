package de.yochyo.ybooru.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException


object Logger{
    private val directory = "$configPath/logs/"
    private val logDirectory = File(directory)
    private val logFile = File("$directory/logcat" + System.currentTimeMillis() + ".txt")

    fun initLogger() {
        if (isExternalStorageWritable()) {
            logDirectory.mkdirs()
            logFile.createNewFile()

            try {
                var process = Runtime.getRuntime().exec("logcat -c")
                process = Runtime.getRuntime().exec("logcat -f ${logFile.absolutePath}")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }


}