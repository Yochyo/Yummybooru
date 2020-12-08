package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2

class AdvancedOnPageChangeCallback<VIEW : View>(private val viewpager: ViewPager2) {
    var onPageScrolled: (position: Int, positionOffset: Float, positionOffsetPixels: Int) -> Unit = { _, _, _ -> }
    var onPageSelected: (oldPosition: Int, position: Int) -> Unit = { _, _ -> }
    var onPageSelected2: ((oldPosition: Int, position: Int, oldView: VIEW?, newView: VIEW?) -> Unit)? = null
    var onPageScrollStateChanged: (state: Int) -> Unit = {}

    var lastItemPosition: Int = 0
    var currentItemPosition: Int = 0


    init {
        viewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                this@AdvancedOnPageChangeCallback.onPageScrollStateChanged(state)
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                this@AdvancedOnPageChangeCallback.onPageScrolled(position, positionOffset, positionOffsetPixels)
            }

            override fun onPageSelected(position: Int) {
                lastItemPosition = currentItemPosition
                currentItemPosition = position
                this@AdvancedOnPageChangeCallback.onPageSelected(lastItemPosition, currentItemPosition)
                this@AdvancedOnPageChangeCallback.onPageSelected2?.invoke(
                    lastItemPosition,
                    currentItemPosition,
                    viewpager.getViewAt(lastItemPosition),
                    viewpager.getViewAt(currentItemPosition)
                )
            }
        })
    }
}

/**
 * Only works if you call "holder.layout.tag = position" on bind of the viewholder
 */
fun <T : View> ViewPager2.getViewAt(position: Int): T? {
    return this.findViewWithTag<ViewGroup?>(position)?.getChildAt(0) as T?
}
