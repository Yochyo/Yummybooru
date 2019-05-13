package de.yochyo.ybooru.utils

import android.content.Context
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.support.v4.provider.DocumentFile
import android.view.MotionEvent
import android.widget.TextView
import de.yochyo.ybooru.api.entities.Server
import java.io.File
import java.security.MessageDigest

fun preview(id: Int) = "${id}P${Server.currentID}"
fun sample(id: Int) = "${id}S${Server.currentID}"
fun original(id: Int) = "${id}O${Server.currentID}"

fun String.toTagArray(): Array<String> = split(" ").toTypedArray()
fun Array<String>.toTagString() = joinToString(" ")
fun List<String>.toTagString() = joinToString(" ")

val configPath = "${Environment.getExternalStorageDirectory().absolutePath}/.yBooru"

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

fun createDefaultSavePath(): String {
    val f = File("${Environment.getExternalStorageDirectory()}/${Environment.DIRECTORY_PICTURES}/yBooru/")
    f.mkdirs()
    return f.absolutePath
}

fun documentFile(context: Context, path: String): DocumentFile? {
    return if (path.startsWith("content")) DocumentFile.fromTreeUri(context, Uri.parse(path))
    else DocumentFile.fromFile(File(path))
}

object Fling {
    fun getDirection(e1: MotionEvent, e2: MotionEvent): Direction {
        val x1 = e1.x
        val y1 = e1.y
        val x2 = e2.x
        val y2 = e2.y
        return getDirection(x1, y1, x2, y2)
    }

    fun getDirection(x1: Float, y1: Float, x2: Float, y2: Float): Direction {
        val angle = getAngle(x1, y1, x2, y2)
        return Direction.fromAngle(angle)
    }


    fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Double {

        val rad = Math.atan2((y1 - y2).toDouble(), (x2 - x1).toDouble()) + Math.PI
        return (rad * 180 / Math.PI + 180) % 360
    }

    enum class Direction {
        up, down, left, right;

        companion object {
            fun fromAngle(angle: Double): Direction {
                return if (inRange(angle, 45f, 135f)) Direction.up
                else if (inRange(angle, 0f, 45f) || inRange(angle, 315f, 360f)) Direction.right
                else if (inRange(angle, 225f, 315f)) Direction.down
                else Direction.left
            }

            private fun inRange(angle: Double, init: Float, end: Float): Boolean {
                return angle >= init && angle < end
            }
        }
    }
}