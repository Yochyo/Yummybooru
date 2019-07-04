package de.yochyo.yummybooru.utils

import android.content.Context
import android.graphics.Bitmap
import android.support.v4.provider.DocumentFile
import de.yochyo.yummybooru.api.downloads.cache
import de.yochyo.yummybooru.api.downloads.downloadImage
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.SafeFileEvent
import kotlinx.coroutines.Dispatchers
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

    suspend fun writeOrDownloadFile(context: Context, post: de.yochyo.yummybooru.api.Post, id: String, url: String) {
        withContext(Dispatchers.IO) {
            val f = context.cache.getCachedBitmap(id)
            if (f != null) writeFile(context, post, f)
            else context.downloadImage(url, id, { writeFile(context, post, it) }, cache = false)
        }
    }

    suspend fun writeFile(context: Context, post: de.yochyo.yummybooru.api.Post, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val file = createFileToWrite(context, post)
            if (file != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                context.contentResolver.openOutputStream(file.uri).write(stream.toByteArray())
                stream.close()
                withContext(Dispatchers.Main){SafeFileEvent.trigger(SafeFileEvent(context, file, post))}
            }
        }
    }

    private suspend fun createFileToWrite(context: Context, post: de.yochyo.yummybooru.api.Post, mimeType: String = post.extension): DocumentFile? {
        return withContext(Dispatchers.IO) {
            val folder = getOrCreateFolder(getParentFolder(context), Server.currentServer.urlHost)
            createFileOrNull(folder, postToFilename(post, mimeType), mimeType)
        }

    }

    private suspend fun postToFilename(p: de.yochyo.yummybooru.api.Post, mimeType: String): String {
        val s = "${Server.currentServer.urlHost} ${p.id} ${p.getTags().joinToString(" ") { it.name }}".filter { it != '/' && it != '\\' && it != '|' && it != ':' && it != '*' && it != '?' && it != '"' && it != '[' && it != ']' }
        var last = s.length
        if (last > 127 - (mimeType.length + 1)) last = 127 - (mimeType.length + 1)
        return s.substring(0, last) + ".$mimeType"
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