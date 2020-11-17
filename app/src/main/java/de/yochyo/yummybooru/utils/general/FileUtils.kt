package de.yochyo.yummybooru.utils.general

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import de.yochyo.booruapi.api.Post
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Resource2
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object FileUtils {

    suspend fun writeBytes(context: Context, documentFile: DocumentFile, input: InputStream): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val stream = context.contentResolver.openOutputStream(documentFile.uri)
                if (stream != null) {
                    stream.use {
                        input.copyTo(it)
                    }
                    return@withContext true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                e.sendFirebase()
            }
            false
        }
    }

    suspend fun writeFile(context: Context, post: Post, res: Resource2, server: Server) {
        withContext(Dispatchers.IO) {
            val file = createFileOrNull(context, post, server, res.mimetype)
            if (file != null) {
                writeBytes(context, file, res.input)
                res.input.close()
            }
        }
    }

    private suspend fun createFileOrNull(context: Context, post: Post, server: Server, mimeType: String): DocumentFile? {
        return createFileOrNull(context, server.urlHost, postToFilename(post, mimeType, server), mimeType)
    }

    suspend fun getFile(context: Context, subfolder: String? = null, name: String): DocumentFile? {
        return withContext(Dispatchers.IO) {
            val root = getOrCreateFolder(context.db.saveFolder, context.getString(R.string.app_name))
            if (root != null) {
                val folder = if (subfolder == null) root else getOrCreateFolder(root, subfolder)
                if (folder != null) return@withContext folder.findFile(name)
            }
            null
        }
    }

    suspend fun createFileOrNull(context: Context, subfolder: String? = null, name: String, mimeType: String): DocumentFile? {
        return withContext(Dispatchers.IO) {
            val folder = context.db.saveFolder
            val root = getOrCreateFolder(context.db.saveFolder, context.getString(R.string.app_name))
            if (root != null) {
                val folder = if (subfolder == null) root else getOrCreateFolder(root, subfolder)
                if (folder != null) return@withContext createFileOrNull(folder, name, mimeType)
            }
            null
        }
    }

    fun getSaveFolderIntent(context: Context) =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION).putExtra("android.content.extra.SHOW_ADVANCED", true)

    fun setSaveFolder(context: Context, uri: Uri) {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.db.saveFolder = DocumentFile.fromTreeUri(context, uri)!!
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