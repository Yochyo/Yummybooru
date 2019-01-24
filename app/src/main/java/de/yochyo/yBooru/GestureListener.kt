package de.yochyo.yBooru

import android.view.GestureDetector
import android.view.MotionEvent

abstract class GestureListener : GestureDetector.SimpleOnGestureListener() {
    abstract fun onSwipe(direction: Direction): Boolean

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return onSwipe(getDirection(e1.x, e1.y, e2.x, e2.y))
    }

    private fun getDirection(x1: Float, y1: Float, x2: Float, y2: Float): Direction {
        val angle = getAngle(x1, y1, x2, y2)
        return Direction.fromAngle(angle)
    }

    private fun getAngle(x1: Float, y1: Float, x2: Float, y2: Float): Double {
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

            private fun inRange(angle: Double, init: Float, end: Float): Boolean = angle >= init && angle < end
        }
    }
}