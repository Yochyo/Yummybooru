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
import de.yochyo.yummybooru.api.entities.Server
import java.io.File
import java.security.MessageDigest
import kotlin.math.atan2

fun Context.preview(id: Int) = "${id}P${Server.getCurrentServerID(this)}"
fun Context.sample(id: Int) = "${id}S${Server.getCurrentServerID(this)}"
fun Context.original(id: Int) = "${id}O${Server.getCurrentServerID(this)}"

fun String.toTagArray(): Array<String> = split(" ").toTypedArray()
fun Array<String>.toTagString() = joinToString(" ")
fun List<String>.toTagString() = joinToString(" ")

val configPath = "${Environment.getExternalStorageDirectory().absolutePath}/.Yummybooru"

fun TextView.setColor(colorCode: Int) {
    if (Build.VERSION.SDK_INT > 22) setTextColor(context.getColor(colorCode))
    else setTextColor(context.resources.getColor(colorCode))
}

private val p = Paint()
fun TextView.underline(underline: Boolean) {
    if (underline) paintFlags = p.apply { isUnderlineText = true }.flags
    else paintFlags = p.apply { isUnderlineText = false }.flags
}

fun passwordToHash(password: String): String {
    val byteArray = "choujin-steiner--$password--".toByteArray(charset = Charsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-1")
    digest.update(byteArray)
    val digestBytes = digest.digest()
    val digestStr = StringBuilder()
    for (b in digestBytes)
        digestStr.append(String.format("%02x", b))
    return digestStr.toString()
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
fun Boolean.toInt() = if(this) 1 else 0
fun createDefaultSavePath(): String {
    val f = File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_PICTURES}/Yummybooru/")
    f.mkdirs()
    return f.absolutePath
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