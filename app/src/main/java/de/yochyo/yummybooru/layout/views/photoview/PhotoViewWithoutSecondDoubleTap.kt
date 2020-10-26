package de.yochyo.yummybooru.layout.views.photoview

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.github.chrisbanes.photoview.OnOutsidePhotoTapListener
import com.github.chrisbanes.photoview.OnPhotoTapListener
import com.github.chrisbanes.photoview.OnViewTapListener
import com.github.chrisbanes.photoview.PhotoView

public class PhotoViewWithoutSecondDoubleTap(context: Context, attr: AttributeSet? = null, defStyle: Int = 0) : PhotoView(context, attr, defStyle) {
    init {
        disableSecondDoubleTap(this)
    }

    private fun disableSecondDoubleTap(view: PhotoView) {
        with(view.attacher) {
            view.attacher.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    val attacher = view.attacher
                    val clazz = attacher::class.java
                    val mOnClickListener = clazz.getDeclaredField("mOnClickListener").apply { isAccessible = true }.get(attacher) as View.OnClickListener?
                    val mViewTapListener = clazz.getDeclaredField("mViewTapListener").apply { isAccessible = true }.get(attacher) as OnViewTapListener?
                    val mPhotoTapListener = clazz.getDeclaredField("mPhotoTapListener").apply { isAccessible = true }.get(attacher) as OnPhotoTapListener?
                    val mOutsidePhotoTapListener = clazz.getDeclaredField("mOutsidePhotoTapListener").apply { isAccessible = true }.get(attacher) as OnOutsidePhotoTapListener?

                    mOnClickListener?.onClick(view)
                    val displayRect = displayRect
                    val x = e.x
                    val y = e.y
                    mViewTapListener?.onViewTap(view, x, y)
                    if (displayRect != null) {
                        // Check to see if the user tapped on the photo
                        if (displayRect.contains(x, y)) {
                            val xResult = ((x - displayRect.left)
                                    / displayRect.width())
                            val yResult = ((y - displayRect.top)
                                    / displayRect.height())
                            mPhotoTapListener?.onPhotoTap(view, xResult, yResult)
                            return true
                        } else
                            mOutsidePhotoTapListener?.onOutsidePhotoTap(view)
                    }
                    return false
                }

                override fun onDoubleTap(ev: MotionEvent): Boolean {
                    try {
                        val scale: Float = getScale()
                        val x = ev.x
                        val y = ev.y
                        if (scale < getMediumScale()) {
                            setScale(getMediumScale(), x, y, true)
                        }
                        /*
                        else if (scale >= getMediumScale() && scale < getMaximumScale()) {
                        setScale(getMaximumScale(), x, y, true);
                    }
                         */
                        else {
                            setScale(getMinimumScale(), x, y, true)
                        }
                    } catch (e: ArrayIndexOutOfBoundsException) {
                        // Can sometimes happen when getX() and getY() is called
                    }
                    return true
                }

                override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                    // Wait for the confirmed onDoubleTap() instead
                    return false
                }
            })
        }
    }
}