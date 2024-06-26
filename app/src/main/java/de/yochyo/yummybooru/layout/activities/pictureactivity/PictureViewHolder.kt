package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.api.TagType
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.layout.views.mediaview.MediaView
import de.yochyo.yummybooru.layout.views.photoview.PhotoViewWithoutSecondDoubleTap
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandAddTag
import de.yochyo.yummybooru.utils.general.downloadAndSaveImage
import de.yochyo.yummybooru.utils.general.loadIntoImageView
import de.yochyo.yummybooru.utils.general.toBooruTag
import de.yochyo.yummybooru.utils.network.downloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
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

    var lastSwipeUp = 0L
    var onSwipe: (GestureListener.Direction) -> Boolean = { direction: GestureListener.Direction ->
        fun onSwipeLeft() = activity.binding.content.viewPager2.currentItem++
        fun onSwipeRight() = activity.binding.content.viewPager2.currentItem--
        fun onSwipeUp() {
            val pos = adapterPosition
            if (pos !in activity.m.posts.indices) return

            val post = activity.m.posts[pos]
            val time = System.currentTimeMillis()
            if (time - lastSwipeUp > 400L) { //download
                downloadAndSaveImage(activity, post)
                GlobalScope.launch(Dispatchers.Main) {
                    val snack = Snackbar.make(viewPager, activity.getString(R.string.download), 150)
                    snack.show()
                }
            } else { //double swipe
                GlobalScope.launch {
                    for (tag in post.getTags().filter { it.tagType == TagType.ARTIST })
                        Command.execute(activity.binding.pictureActivityContainer, CommandAddTag(tag.toBooruTag(activity.viewModel.server)))
                }
            }
            lastSwipeUp = time

        }
        when (direction) {
            GestureListener.Direction.DOWN -> activity.finish()
            GestureListener.Direction.UP -> onSwipeUp()
            GestureListener.Direction.LEFT -> onSwipeLeft()
            GestureListener.Direction.RIGHT -> onSwipeRight()
        }
        true
    }


    private val viewPager = activity.binding.content.viewPager2

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
            downloader.download(post.fileSampleURL, {
                mutex.withLock {
                    downloadedOriginalImage = it != null
                    it?.loadIntoImageView(photoView)
                }
            }, activity.viewModel.server.headers, downloadNow = true)
            downloader.downloadPostPreviewIMG(activity, post, {
                mutex.withLock {
                    if (!downloadedOriginalImage && it != null)
                        GlobalScope.launch(Dispatchers.Main) { photoView.setImageBitmap(it.bitmap) }
                }
            }, activity.viewModel.server.headers, downloadFirst = true)
        }
        currentChild = photoView
    }

    fun loadVideo(post: Post) {
        mediaView.setVideoPath(post.fileSampleURL, activity.viewModel.server.api.getHeaders())
        currentChild = mediaView
    }

    private fun createPhotoView(): PhotoView {
        val view = PhotoViewWithoutSecondDoubleTap(activity)
        view.setOnScaleChangeListener { scaleFactor, _, _ ->
            activity.binding.content.viewPager2.isUserInputEnabled = scaleFactor != 1f
        }
        view.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        view.setAllowParentInterceptOnEdge(true)
        view.setOnSingleFlingListener { e1, e2, _, _ ->
            onSwipe(GestureListener.getDirection(e1, e2))
        }
        view.setOnClickListener { onClick(adapterPosition) }
        return view
    }


    private fun createMediaView(): MediaView {
        val view = MediaView(activity)
        val detector = GestureDetector(activity, object : GestureListener() {
            override fun onSwipe(direction: Direction): Boolean {
                return this@PictureViewHolder.onSwipe(direction)
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                onClick(adapterPosition)
                return true
            }
        })
        layout.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
        return view
    }

    fun isZoomed() = getPhotoViewIfVisible()?.scale == 1.0f
    fun resume() = getMediaViewIfVisible()?.resume()
    fun pause() = getMediaViewIfVisible()?.pause()
    fun destroy() = getMediaViewIfVisible()?.destroy()

    fun getPhotoViewIfVisible(): PhotoView? {
        val child = currentChild
        return if (child != null && child is PhotoView) child
        else null
    }

    fun getMediaViewIfVisible(): MediaView? {
        val child = currentChild
        return if (child != null && child is MediaView) child
        else null
    }

    private fun onClick(position: Int) {
        if (activity.preferences.clickToMoveToNextPicture)
            viewPager.currentItem = position + 1
    }
}