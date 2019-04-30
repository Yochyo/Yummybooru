package de.yochyo.ybooru.utils

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.IOException


abstract class Logger(directory: String) {
    companion object{
        private var _instance : Logger?= null
                fun initLogger(){
                    if(_instance == null){
                        _instance = object: Logger("${Environment.getExternalStorageDirectory().absolutePath}/.yBooru/logs/"){}
                        _instance!!.start()
                    }
                }
    }
    private val logDirectory = File(directory)
    private val logFile = File("$directory/logcat" + System.currentTimeMillis() + ".txt")

    fun start() {
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