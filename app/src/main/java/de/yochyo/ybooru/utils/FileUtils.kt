package de.yochyo.ybooru.utils

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.provider.DocumentFile
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.api.entities.Server
import de.yochyo.ybooru.api.downloads.cache
import de.yochyo.ybooru.api.downloads.downloadImage
import de.yochyo.ybooru.database.db
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

object FileUtils {
    private var oldSavePath: String? = null
    private var parentFolder: DocumentFile? = null
    fun getParentFolder(context: Context): DocumentFile {
        if (oldSavePath == null) { //Initialisieren beim ersten mal
            oldSavePath = db.savePath
            parentFolder = documentFile(context, oldSavePath!!)
        } //Falls der Speicherpfad geändert wird
        if (oldSavePath != db.savePath) {
            oldSavePath = db.savePath
            parentFolder = documentFile(context, oldSavePath!!)
        } //Falls der Pfad nicht mehr existiert
        if (parentFolder == null || !parentFolder!!.exists()) {
            oldSavePath = createDefaultSavePath()
            db.savePath = oldSavePath!!
            parentFolder = documentFile(context, oldSavePath!!)
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
            val folder = getOrCreateFolder(getParentFolder(context), Server.currentServer.urlHost)
            createFileOrNull(folder, postToFilename(post), "png")
        }

    }

    private suspend fun postToFilename(p: Post): String {
        val s = "${Server.currentServer.urlHost} ${p.id} ${p.getTags().joinToString(" ") { it.name }}".filter { it != '/' && it != '\\' && it != '|' && it != ':' && it != '*' && it != '?' && it != '"' && it != '[' && it != ']' }
        var last = s.length
        if (last > 123) last = 123
        return s.substring(0, last) + ".png"
    }

    private fun createFileOrNull(parent: DocumentFile, name: String, mimeType: String): DocumentFile? {
        val file = parent.findFile(name)
        return if (file != null) null
        else return parent.createFile(mimeType, name)!!
    }

    private fun getOrCreateFolder(parent: DocumentFile, name: String): DocumentFile {
        val file = parent.findFile(name)
        return if (file != null) file
        else parent.createDirectory(name)!!
    }
}