package de.yochyo.yummybooru.utils.general

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.os.Build
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
import de.yochyo.yummybooru.api.entities.IResource
import de.yochyo.yummybooru.api.entities.Resource2
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.manager.ManagerWrapper
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.downloadservice.saveDownload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

fun Context.preview(id: Int) = "${id}p${db.selectedServerValue.id}"
fun Context.sample(id: Int) = "${id}s${db.selectedServerValue.id}"
fun Context.original(id: Int) = "${id}o${db.selectedServerValue.id}"

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
        saveDownload(context, url, id, context.db.selectedServerValue, post)
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

fun Tag.toBooruTag(server: Server) = de.yochyo.yummybooru.api.entities.Tag(name, tagType, server.id, count = count)

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


fun <E> List<E>.copy() = this.map { it }
fun <E> List<E>.addToCopy(element: E) = copy().apply { (this as MutableList<E>).add(element) }
fun <E> List<E>.addToCopy(element: List<E>) = copy().apply { (this as MutableList<E>).addAll(element) }
fun <E> List<E>.removeFromCopy(element: E) = copy().apply { (this as MutableList<E>).remove(element) }