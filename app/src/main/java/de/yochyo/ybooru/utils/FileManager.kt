package de.yochyo.ybooru.utils

import android.graphics.Bitmap
import android.os.Environment
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.database.entities.Server
import java.io.ByteArrayOutputStream
import java.io.File

object FileManager {
    private val saveDirectory: String = "${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_PICTURES}/yBooru"
    private val files = ArrayList<Int>()

    init {
        val f = File(saveDirectory)
        f.mkdirs()
        if (f.isDirectory)
            for (f in f.listFiles())
                try {
                    files += f.name.substring(0, f.name.indexOf(' ')).toInt()
                } catch (e: Exception) {
                }
    }

    fun writeFile(post: Post, bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val file = File("$saveDirectory/${postToFilename(post)}")
        file.createNewFile()
        file.writeBytes(stream.toByteArray())
    }

    private fun postToFilename(p: Post): String {
        val s = "${Server.currentServer.url} ${p.id} ${p.tags.value.joinToString(" ") { it.name }}".filter { it != '/' && it != '\\' && it != '|' && it != ':' && it != '*' && it != '?' && it != '"' && it != '<' && it != '>' }
        var last = s.lastIndex
        if (last > 123) last = 123
        return "${s.substring(0, last)}.png"
    }
}