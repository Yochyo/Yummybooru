package de.yochyo.yummybooru.utils.general

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.view.MotionEvent
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import de.yochyo.booruapi.objects.Post
import de.yochyo.booruapi.objects.Tag
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Sub
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.saveDownload
import de.yochyo.yummybooru.utils.ManagerWrapper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest
import kotlin.math.atan2

fun Context.preview(id: Int) = "${id}P${currentServer.id}"
fun Context.sample(id: Int) = "${id}S${currentServer.id}"
fun Context.original(id: Int) = "${id}O${currentServer.id}"

fun String.toTagArray(): Array<String> = split(" ").toTypedArray()
fun Array<String>.toTagString() = joinToString(" ")
fun List<String>.toTagString() = joinToString(" ")

val configPath = "${Environment.getExternalStorageDirectory().absolutePath}/.Yummybooru"

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
fun getDownloadPathAndId(context: Context, p: Post): Pair<String, String>{
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

fun Tag.toBooruTag(context: Context) = de.yochyo.yummybooru.api.entities.Tag(name, type, false, count, null, serverID = context.currentServer.id)

suspend fun de.yochyo.yummybooru.api.entities.Tag.addSub(context: Context): Boolean {
    val s = context.currentServer
    val t = s.getTag(context, name)
    val id = s.newestID()
    return if (t != null && id != null) {
        this.sub = Sub(t.count, id)
        true
    } else false
}

suspend fun createTagAndOrChangeSubState(context: Context, name: String): de.yochyo.yummybooru.api.entities.Tag? {
    val db = context.db
    val tag = db.getTag(name)
    if (tag != null) {
        if (tag.sub == null) tag.addSub(context)
        else tag.sub = null
        return tag
    } else {
        val t = context.currentServer.getTag(context, name)
        if (t != null) {
            t.addSub(context)
            db.tags += t
            return t
        }
    }
    return null
}

fun parseURL(url: String): String {
    val b = StringBuffer()
    if (!url.startsWith("http"))
        b.append("https://")
    b.append(url)
    if (!url.endsWith("/"))
        b.append("/")
    return b.toString()
}

fun Boolean.toInt() = if (this) 1 else 0
fun createDefaultSavePath(): String {
    val f = File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_PICTURES}/Yummybooru/")
    f.mkdirs()
    return f.absolutePath
}

class MutablePair<A, B>(var first: A, var second: B)

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


private var _currentServer: Server? = null
val Context.currentServer: Server
    get() {
        val s = _currentServer
        if (s == null || s.id != db.currentServerID)
            _currentServer = db.getServer(db.currentServerID)
                    ?: if (db.servers.isNotEmpty()) db.servers.first() else null

        return _currentServer ?: Server("", "", "", "", "")
    }

private var _currentManager: ManagerWrapper? = null
var Context.currentManager: ManagerWrapper
    get() {
        val v = if (_currentManager == null) ManagerWrapper(ManagerWrapper.build(this, "*")) else _currentManager
        _currentManager = null
        return v!!
    }
    set(value) {
        _currentManager = value
    }

object Fling {
    fun getDirection(e1: MotionEvent, e2: MotionEvent): Direction {
        val x1 = e1.x
        val y1 = e1.y
        val x2 = e2.x
        val y2 = e2.y
        return getDirection(x1, y1, x2, y2)
    }

    private fun getDirection(x1: Float, y1: Float, x2: Float, y2: Float): Direction {
        val angle = getAngle(x1, y1, x2, y2)
        return Direction.fromAngle(angle)
    }


    private fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Double {

        val rad = atan2((y1 - y2).toDouble(), (x2 - x1).toDouble()) + Math.PI
        return (rad * 180 / Math.PI + 180) % 360
    }

    enum class Direction {
        UP, DOWN, LEFT, RIGHT;

        companion object {
            fun fromAngle(angle: Double): Direction {
                return if (inRange(angle, 45f, 135f)) UP
                else if (inRange(angle, 0f, 45f) || inRange(angle, 315f, 360f)) RIGHT
                else if (inRange(angle, 225f, 315f)) DOWN
                else LEFT
            }

            private fun inRange(angle: Double, init: Float, end: Float): Boolean {
                return angle >= init && angle < end
            }
        }
    }

}