package de.yochyo.yummybooru.utils.general

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Build
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.api.Tag
import de.yochyo.booruapi.manager.ManagerOR
import de.yochyo.booruapi.manager.ManagerORAlternatingAlgorithm
import de.yochyo.booruapi.manager.ManagerORDefaultSortingAlgorithm
import de.yochyo.booruapi.manager.ManagerORRandomSortingAlgorithm
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.IResource
import de.yochyo.yummybooru.api.entities.Resource2
import de.yochyo.yummybooru.api.manager.ManagerWrapper
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.downloadservice.saveDownload
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandAddTag
import de.yochyo.yummybooru.utils.commands.CommandFavoriteTag
import de.yochyo.yummybooru.utils.commands.CommandUpdateFollowingTagData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

fun Context.preview(id: Int) = "${id}p${db.currentServer.id}"
fun Context.sample(id: Int) = "${id}s${db.currentServer.id}"
fun Context.original(id: Int) = "${id}o${db.currentServer.id}"

fun String.toTagArray(): Array<String> = split(" ").toTypedArray()
fun Array<String>.toTagString() = joinToString(" ")
fun List<String>.toTagString() = joinToString(" ")

fun TextView.setColor(colorCode: Int) {
    if (Build.VERSION.SDK_INT > 22) setTextColor(context.getColor(colorCode))
    else setTextColor(context.resources.getColor(colorCode))
}

fun downloadAndSaveImage(context: Context, post: Post) {
    val (url, id) = getDownloadPathAndId(context, post)
    GlobalScope.launch {
        saveDownload(context, url, id, context.db.currentServer, post)
    }
}

fun updateNomediaFile(context: Context, newValue: Boolean? = null) {
    GlobalScope.launch {
        if (newValue == true || context.preferences.useNomedia) FileUtils.createFileOrNull(context, null, ".nomedia", "")
        else FileUtils.getFile(context, null, ".nomedia")?.delete()
    }
}

fun getDownloadPathAndId(context: Context, p: Post): Pair<String, String> {
    val url: String
    val id: String
    if (context.preferences.downloadOriginal) {
        if (p.fileURL.mimeType == "zip" && context.preferences.downloadWebm) {
            url = p.fileSampleURL
            id = context.sample(p.id)
        } else {
            url = p.fileURL
            id = context.original(p.id)
        }
    } else {
        url = p.fileSampleURL
        id = context.sample(p.id)
    }
    return Pair(url, id)
}

private val p = Paint()
fun TextView.underline(underline: Boolean) {
    if (underline) paintFlags = p.apply { isUnderlineText = true }.flags
    else paintFlags = p.apply { isUnderlineText = false }.flags
}

fun Tag.toBooruTag(context: Context) = de.yochyo.yummybooru.api.entities.Tag(name, tagType, false, count, null, serverId = context.db.currentServer.id)

suspend fun de.yochyo.yummybooru.api.entities.Tag.addFollowing(viewForSnackbar: View): Boolean {
    val following = getFollowingData(viewForSnackbar.context, this) ?: return false
    Command.execute(viewForSnackbar, CommandUpdateFollowingTagData(this, following))
    return true
}

suspend fun getFollowingData(context: Context, tag: de.yochyo.yummybooru.api.entities.Tag): Following? {
    val s = context.db.currentServer
    val t = s.getTag(context, tag.name)
    val id = s.newestID()
    return if (id != null) Following(id, t.count)
    else null
}

suspend fun createTagAndOrChangeFavoriteSate(viewForSnackbar: View, name: String) {
    withContext(TagDispatcher) {
        val context = viewForSnackbar.context
        val tag = context.db.getTag(name)

        if (tag == null) Command.execute(viewForSnackbar, CommandAddTag(context.db.currentServer.getTag(context, name).copy(isFavorite = true)))
        else Command.execute(viewForSnackbar, CommandFavoriteTag(tag, !tag.isFavorite))
    }
}

suspend fun createTagAndOrChangeFollowingState(viewForSnackbar: View, name: String): de.yochyo.yummybooru.api.entities.Tag {
    return withContext(TagDispatcher) {
        val context = viewForSnackbar.context
        viewForSnackbar.context.db.getTag(name)?.apply {
            if (following == null) addFollowing(viewForSnackbar)
            else Command.execute(viewForSnackbar, CommandUpdateFollowingTagData(this, null))
        } ?: context.db.currentServer.getTag(context, name).let { it.copy(following = getFollowingData(context, it)) }.apply {
            Command.execute(viewForSnackbar, CommandAddTag(this))
        }
    }
}

val Fragment.ctx: Context get() = this.requireContext()
fun parseURL(url: String): String {
    val b = StringBuffer()
    if (!url.startsWith("http"))
        b.append("https://")
    b.append(url)
    if (!url.endsWith("/"))
        b.append("/")
    return b.toString()
}

fun updateCombinedSearchSortAlgorithm(value: Int) {
    ManagerOR.defaultSortingAlgerithm = when (value) {
        -1 -> ManagerORRandomSortingAlgorithm()
        in 1..10 -> ManagerORAlternatingAlgorithm(value)
        else -> ManagerORDefaultSortingAlgorithm()
    }
}

fun InputStream.toBitmap(): Bitmap? {
    var result: Bitmap? = null
    try {
        result = BitmapFactory.decodeStream(this)

    } catch (e: java.lang.Exception) {
        e.printStackTrace()
        e.sendFirebase()
    } finally {
        close()
    }
    return result
}

fun ByteArray.toBitmap() = BitmapFactory.decodeByteArray(this, 0, this.size)

suspend fun Resource2.loadIntoImageView(imageView: ImageView) {
    when (type) {
        IResource.IMAGE -> {
            withContext(Dispatchers.IO) {
                val bitmap = input.toBitmap()
                withContext(Dispatchers.Main) { imageView.setImageBitmap(bitmap) }
            }
        }
        IResource.ANIMATED -> try {
            withContext(Dispatchers.IO) {
                val byteArray = input.use { it.readBytes() }
                withContext(Dispatchers.Main) {
                    Glide.with(imageView).load(byteArray).into(imageView)
                }
            }
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}

val String.mimeType: String?
    get() {
        val lastIndex = lastIndexOf(".")
        return if (lastIndex != -1) substring(lastIndexOf(".") + 1)
        else null
    }

fun Context.drawable(id: Int) = ContextCompat.getDrawable(this, id)

suspend fun ManagerWrapper.restoreManager(context: Context, lastId: Int, lastPosition: Int) {
    if (lastId > 0) {
        downloadNextPages(lastPosition / context.preferences.limit + 1)
        while (this.posts.indexOfFirst { it.id == lastId } == -1) downloadNextPage()
        this.position = this.posts.indexOfFirst { it.id == lastId }
    }
}