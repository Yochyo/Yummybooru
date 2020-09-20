package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.view.*
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.booruapi.objects.Post
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.views.mediaview.MediaView
import de.yochyo.yummybooru.utils.general.downloadImage
import de.yochyo.yummybooru.utils.general.loadIntoImageView
import de.yochyo.yummybooru.utils.general.preview
import de.yochyo.yummybooru.utils.general.toBooruTag
import de.yochyo.yummybooru.utils.network.download
import de.yochyo.yummybooru.utils.network.downloadBitmap
import kotlinx.android.synthetic.main.picture_activity_drawer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class PictureViewHolder(
    val activity: PictureActivity,
    val layout: ViewGroup = LinearLayout(activity).apply {
        gravity = Gravity.CENTER
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
    }
) : RecyclerView.ViewHolder(layout) {
    private val viewPager = activity.view_pager2

    private val photoView = createPhotoView()
    private var mediaView: MediaView = createMediaView()


    private var currentChild: View?
        get() = layout.getChildAt(0)
        set(value) {
            destroy()
            layout.removeAllViews()
            layout.addView(value)
        }


    fun loadImage(post: Post) {
        GlobalScope.launch {
            var downloadedOriginalImage = false
            val mutex = Mutex()
            download(post.fileSampleURL, {
                GlobalScope.launch(Dispatchers.Main) {
                    mutex.withLock {
                        downloadedOriginalImage = true
                        it.loadIntoImageView(photoView)
                    }
                }
            }, downloadNow = true)
            downloadBitmap(activity, post.filePreviewURL, activity.preview(post.id), {
                GlobalScope.launch(Dispatchers.Main) {
                    mutex.withLock {
                        if (!downloadedOriginalImage) it.loadIntoImageView(photoView)
                    }
                }
            }, downloadFirst = true)
        }
        currentChild = photoView
    }

    fun loadVideo(post: Post) {
        mediaView.setVideoPath(post.fileSampleURL)
        currentChild = mediaView
    }

    private fun createPhotoView(): PhotoView {
        val view = PhotoView(activity)
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        view.setAllowParentInterceptOnEdge(true)
        view.setOnSingleFlingListener { e1, e2, velocityX, velocityY ->
            when (GestureListener.getDirection(e1, e2)) {
                GestureListener.Direction.DOWN -> onFlingDown()
                GestureListener.Direction.UP -> onFlingUp(activity.m.posts[adapterPosition])
                else -> false
            }
        }
        view.setOnClickListener { onClick(adapterPosition) }
        return view
    }

    private fun createMediaView(): MediaView {
        val view = MediaView(activity)
        val detector = GestureDetector(activity, object : GestureListener() {
            override fun onSwipe(direction: Direction): Boolean {
                return when (direction) {
                    Direction.UP -> onFlingUp(activity.m.posts[adapterPosition])
                    Direction.DOWN -> onFlingDown()
                    else -> false
                }
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                onClick(adapterPosition)
                return true
            }
        })
        layout.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
        return view
    }

    fun resume() {
        val child = currentChild
        if (child != null && child is MediaView) {
            child.resume()
        }
    }

    fun pause() {
        val child = currentChild
        if (child != null && child is MediaView) {
            child.pause()
        }
    }

    fun destroy() {
        val child = currentChild
        if (child != null && child is MediaView) {
            child.destroy()
        }
    }

    var lastSwipeUp = 0L
    private fun onFlingUp(post: Post): Boolean {
        val time = System.currentTimeMillis()
        if (time - lastSwipeUp > 400L) { //download
            downloadImage(activity, post)
            val snack = Snackbar.make(viewPager, activity.getString(R.string.download), Snackbar.LENGTH_SHORT)
            snack.show()
            GlobalScope.launch(Dispatchers.Main) {
                delay(150)
                snack.dismiss()
            }
        } else { //double swipe
            GlobalScope.launch {
                for (tag in post.tags.filter { it.type == Tag.ARTIST })
                    activity.db.tags += tag.toBooruTag(activity)
            }
        }
        lastSwipeUp = time
        return true
    }

    private fun onFlingDown(): Boolean {
        activity.finish()
        return true
    }

    private fun onClick(position: Int) {
        if (activity.db.clickToMoveToNextPicture)
            viewPager.currentItem = position + 1
    }
}