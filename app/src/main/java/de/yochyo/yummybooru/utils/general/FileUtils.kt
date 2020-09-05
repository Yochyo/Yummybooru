package de.yochyo.yummybooru.utils.general

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import de.yochyo.booruapi.objects.Post
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Resource
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FileUtils {

    suspend fun writeBytes(context: Context, documentFile: DocumentFile, bytes: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openOutputStream(documentFile.uri)
                if (stream != null) {
                    stream.write(bytes)
                    stream.close()
                    return@withContext true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e.log()
            }
            false
        }
    }

    suspend fun writeFile(context: Context, post: Post, res: Resource, server: Server) {
        withContext(Dispatchers.IO) {
            val file = createFileOrNull(context, post, server, res.mimetype)
            if (file != null) {
                writeBytes(context, file, res.resource)
            }
        }
    }

    private suspend fun createFileOrNull(context: Context, post: Post, server: Server, mimeType: String): DocumentFile? {
        return createFileOrNull(context, server.urlHost, postToFilename(post, mimeType, server), mimeType)
    }

    public suspend fun getFile(context: Context, subfolder: String? = null, name: String): DocumentFile? {
        return withContext(Dispatchers.IO) {
            val root = getOrCreateFolder(context.db.saveFolder, context.getString(R.string.app_name))
            if (root != null) {
                val folder = if (subfolder == null) root else getOrCreateFolder(root, subfolder)
                if (folder != null) return@withContext folder.findFile(name)
            }
            null
        }
    }

    public suspend fun createFileOrNull(context: Context, subfolder: String? = null, name: String, mimeType: String): DocumentFile? {
        return withContext(Dispatchers.IO) {
            val root = getOrCreateFolder(context.db.saveFolder, context.getString(R.string.app_name))
            if (root != null) {
                val folder = if (subfolder == null) root else getOrCreateFolder(root, subfolder)
                if (folder != null) return@withContext createFileOrNull(folder, name, mimeType)
            }
            null
        }
    }

    private fun postToFilename(p: Post, mimeType: String, server: Server): String {
        val s =
            "${server.urlHost} ${p.id} ${p.tagString}".filter { it != '/' && it != '\\' && it != '|' && it != ':' && it != '*' && it != '?' && it != '"' && it != '[' && it != ']' }
        var last = s.length
        if (last > 127 - (mimeType.length + 1)) last = 127 - (mimeType.length + 1)
        return s.substring(0, last) + ".$mimeType"
    }

    private fun createFileOrNull(parent: DocumentFile, name: String, mimeType: String): DocumentFile? {
        val file = parent.findFile(name)
        return if (file != null) null
        else return parent.createFile(mimeType, name)
    }

    private fun getOrCreateFolder(parent: DocumentFile, name: String): DocumentFile? {
        return parent.findFile(name) ?: parent.createDirectory(name)
    }
}