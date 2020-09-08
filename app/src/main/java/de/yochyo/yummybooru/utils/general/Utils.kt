package de.yochyo.yummybooru.utils.general

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.google.firebase.crashlytics.FirebaseCrashlytics
import de.yochyo.booruapi.objects.Post
import de.yochyo.booruapi.objects.Tag
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.saveDownload
import de.yochyo.yummybooru.utils.ManagerWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

fun downloadImage(context: Context, p: Post) {
    val (url, id) = getDownloadPathAndId(context, p)
    GlobalScope.launch {
        saveDownload(context, url, id, p)
    }
}

fun updateNomediaFile(context: Context, newValue: Boolean? = null) {
    GlobalScope.launch {
        if (newValue == true || context.db.useNomedia) FileUtils.createFileOrNull(context, null, ".nomedia", "")
        else FileUtils.getFile(context, null, ".nomedia")?.delete()
    }
}

fun Exception.log() = FirebaseCrashlytics.getInstance().recordException(this)
fun Error.log() = FirebaseCrashlytics.getInstance().recordException(this)

fun getDownloadPathAndId(context: Context, p: Post): Pair<String, String> {
    val url: String
    val id: String
    if (context.db.downloadOriginal) {
        if (p.fileURL.mimeType == "zip" && context.db.downloadWebm) {
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

fun Tag.toBooruTag(context: Context) = de.yochyo.yummybooru.api.entities.Tag(name, type, false, count, null, serverID = context.db.currentServer.id)

suspend fun de.yochyo.yummybooru.api.entities.Tag.addFollowing(context: Context): Boolean {
    val s = context.db.currentServer
    val t = s.getTag(context, name)
    val id = s.newestID()
    return if (t != null && id != null) {
        this.following = Following(id, t.count)
        true
    } else false
}

suspend fun createTagAndOrChangeFollowingState(context: Context, name: String): de.yochyo.yummybooru.api.entities.Tag? {
    val db = context.db
    val tag = db.getTag(name)
    if (tag != null) {
        if (tag.following == null) tag.addFollowing(context)
        else tag.following = null
        return tag
    } else {
        val t = context.db.currentServer.getTag(context, name)
        if (t != null) {
            t.addFollowing(context)
            db.tags += t
            return t
        }
    }
    return null
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

fun documentFile(context: Context, path: String): DocumentFile {
    return if (path.startsWith("content")) DocumentFile.fromTreeUri(context, Uri.parse(path))!!
    else DocumentFile.fromFile(File(path))
}

fun ByteArray.toBitmap() = BitmapFactory.decodeByteArray(this, 0, this.size)
val String.mimeType: String?
    get() {
        val lastIndex = lastIndexOf(".")
        return if (lastIndex != -1) substring(lastIndexOf(".") + 1)
        else null
    }

fun Context.drawable(id: Int) = ContextCompat.getDrawable(this, id)


private val _managers = HashMap<String, ManagerWrapper?>()
private var _currentManager: ManagerWrapper? = null
    set(value) {
        _managers[value.toString()] = value
        field = value
    }

fun Context.getCurrentManager(name: String? = null): ManagerWrapper? {
    val m = if (name == null) _currentManager
    else _managers[name]
    return m
}

suspend fun Context.getOrRestoreManager(name: String, lastId: Int, lastPosition: Int): ManagerWrapper {
    return withContext(Dispatchers.IO) {
        val m: ManagerWrapper
        val manager = getCurrentManager(name)
        if (manager?.toString() == name) m = manager
        else {
            m = ManagerWrapper.build(this@getOrRestoreManager, name)
            if (lastId > 0) {
                m.downloadNextPages(lastPosition / db.limit + 1)
                while (m.posts.indexOfFirst { it.id == lastId } == -1) m.downloadNextPage()
                m.position = m.posts.indexOfFirst { it.id == lastId }
            }
        }
        setCurrentManager(m)
        m
    }
}

fun Context.setCurrentManager(m: ManagerWrapper) {
    _currentManager = m
}

fun clearCurrentManager() {
    _managers.clear()
    _currentManager = null
}