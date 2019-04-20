package de.yochyo.ybooru.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.api.downloadImage
import de.yochyo.ybooru.api.downloader
import de.yochyo.ybooru.database.entities.Server
import java.io.ByteArrayOutputStream
import java.io.File

object FileUtils {
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

    suspend fun writeOrDownloadFile(context: Context, post: Post, id: String, url: String) {
        val f = context.downloader.getCachedFile(id)
        if (f != null)
            writeFile(post, f)
        else
            context.downloadImage(url, id, { FileUtils.writeFile(post, it) }, cache = false)
    }

    suspend fun writeFile(post: Post, bitmap: Bitmap) {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val file = File("$saveDirectory/${postToFilename(post)}")
        file.createNewFile()
        file.writeBytes(stream.toByteArray())
    }

    suspend fun writeFile(post: Post, bitmap: File) {
        val file = File("$saveDirectory/${postToFilename(post)}")
        file.createNewFile()
        file.writeBytes(file.readBytes())
    }

    private fun postToFilename(p: Post): String {
        val s = "${Server.currentServer.url} ${p.id} ${p.tags.value.joinToString(" ") { it.name }}".filter { it != '/' && it != '\\' && it != '|' && it != ':' && it != '*' && it != '?' && it != '"' && it != '<' && it != '>' }
        var last = s.lastIndex
        if (last > 123) last = 123
        return "${s.substring(0, last)}.png"
    }
}