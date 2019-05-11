package de.yochyo.ybooru.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.provider.DocumentFile
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.api.cache
import de.yochyo.ybooru.api.downloadImage
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.database.entities.Server
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object FileUtils {

    suspend fun writeOrDownloadFile(context: Context, post: Post, id: String, url: String, doAfter: suspend CoroutineScope.() -> Unit = {}) {
        withContext(Dispatchers.IO) {
            val f = context.cache.getCachedBitmap(id)
            if (f != null) {
                writeFile(context, post, f)
                launch(Dispatchers.Main) { doAfter() }
            } else context.downloadImage(url, id, { writeFile(context, post, it);doAfter() }, cache = false)

        }
    }

    suspend fun writeFile(context: Context, post: Post, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val file = createFileToWrite(context, post)
            context.contentResolver.openOutputStream(file.uri).write(stream.toByteArray())
            stream.close()
        }
    }

    private suspend fun createFileToWrite(context: Context, post: Post): DocumentFile {
        return withContext(Dispatchers.IO) {
            val parentFolder = DocumentFile.fromTreeUri(context, Uri.parse(db.getSavePath(context)))
            if(parentFolder != null){
                val folder = parentFolder.createDirectory(Server.currentServer.urlHost)
                if(folder != null){
                    val file = folder.createFile("png", postToFilename(post))
                    if(file != null)
                        return@withContext file!!
                }
            }
        throw Exception("error when creating file")
        }
    }

    private suspend fun postToFilename(p: Post): String {
        val s = "${Server.currentServer.urlHost} ${p.id} ${p.getTags().joinToString(" ") { it.name }}".filter { it != '/' && it != '\\' && it != '|' && it != ':' && it != '*' && it != '?' && it != '"' && it != '[' && it != ']' }
        var last = s.length
        if (last > 123) last = 123
        return s.substring(0, last)
    }
}