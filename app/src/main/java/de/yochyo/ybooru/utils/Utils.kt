package de.yochyo.ybooru.utils

import android.graphics.Paint
import android.os.Build
import android.view.MotionEvent
import android.widget.TextView
import de.yochyo.ybooru.api.Tag
import java.util.*

fun large(id: Int) = "${id}Large"
fun preview(id: Int) = "${id}Preview"
fun original(id: Int) = "${id}Original"
fun large(id: String) = "${id}Large"
fun preview(id: String) = "${id}Preview"
fun original(id: String) = "${id}Original"

fun String.toTagArray(): Array<String> = split(" ").toTypedArray()
fun Array<String>.toTagString() = joinToString(" ")
fun List<String>.toTagString() = joinToString(" ")

fun TreeSet<Tag>.get(name: Int) = find { it.name == name.toString() }

fun TextView.setColor(tag: Tag) {
    if (Build.VERSION.SDK_INT > 22) setTextColor(context.getColor(tag.color))
    else setTextColor(context.resources.getColor(tag.color))
}

fun TextView.underline(underline: Boolean) {
    if (underline) paintFlags = Paint().apply { isUnderlineText = true }.flags
    else paintFlags = Paint().apply { isUnderlineText = false }.flags
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