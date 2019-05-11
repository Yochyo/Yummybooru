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
    private var oldSavePath: String? = null
    private var parentFolder: DocumentFile? = null
    fun getParentFolder(context: Context): DocumentFile {
        if (oldSavePath == null) {
            oldSavePath = db.getSavePath(context)
            println(oldSavePath)
            parentFolder = DocumentFile.fromTreeUri(context, Uri.parse(oldSavePath))
        }
        if (parentFolder == null || !parentFolder!!.exists()) {
            oldSavePath = createDefaultSavePath(context)
            db.setSavePath(oldSavePath!!)
            parentFolder = DocumentFile.fromTreeUri(context, Uri.parse(oldSavePath))
        }
        return parentFolder!!
    }

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
            val file = createFileToWrite(context, post)
            if (file != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                context.contentResolver.openOutputStream(file.uri).write(stream.toByteArray())
                stream.close()
            }
        }
    }

    private suspend fun createFileToWrite(context: Context, post: Post): DocumentFile? {
        return withContext(Dispatchers.IO) {
            var folder = getParentFolder(context).findFile(Server.currentServer.urlHost)
            if (folder == null)
                folder = getParentFolder(context).createDirectory(Server.currentServer.urlHost)!!
            val fileName = postToFilename(post)
            val file = folder.findFile(fileName)
            if(file == null) return@withContext folder.createFile("png", fileName)
            return@withContext null
        }

    }

    private suspend fun postToFilename(p: Post): String {
        val s = "${Server.currentServer.urlHost} ${p.id} ${p.getTags().joinToString(" ") { it.name }}".filter { it != '/' && it != '\\' && it != '|' && it != ':' && it != '*' && it != '?' && it != '"' && it != '[' && it != ']' }
        var last = s.length
        if (last > 123) last = 123
        return s.substring(0, last) + ".png"
    }
}