package de.yochyo.ybooru.utils

import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.api.cache
import de.yochyo.ybooru.api.downloadImage
import de.yochyo.ybooru.database.entities.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object FileUtils {
    private val saveDirectory: String = "${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_PICTURES}/yBooru/"

    suspend fun writeOrDownloadFile(context: Context, post: Post, id: String, url: String, doAfter: suspend CoroutineScope.() -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val f = context.cache.getCachedBitmap(id)
            if (f != null) {
                writeFile(post, f)
                launch(Dispatchers.Main) { doAfter() }
            } else context.downloadImage(url, id, { writeFile(post, it);doAfter() }, cache = false)

        }
    }

    suspend fun writeFile(post: Post, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            createFileToWrite(post).writeBytes(stream.toByteArray())
            stream.close()
        }
    }

    private fun createFileToWrite(post: Post): File {
        val folder = File("$saveDirectory${Server.currentServer.urlHost}/")
        folder.mkdirs()
        val f = File("${folder.absolutePath}/${postToFilename(post)}")
        f.createNewFile()
        return f
    }

    private fun postToFilename(p: Post): String {
        val s = "${Server.currentServer.urlHost} ${p.id} ${p.tags.joinToString(" ") { it.name }}".filter { it != '/' && it != '\\' && it != '|' && it != ':' && it != '*' && it != '?' && it != '"' && it != '<' && it != '>' }
        var last = s.length
        if (last > 123) last = 123
        return "${s.substring(0, last)}.png"
    }
}