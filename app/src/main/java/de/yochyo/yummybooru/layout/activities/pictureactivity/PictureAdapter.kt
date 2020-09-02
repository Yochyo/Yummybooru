package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.view.*
import android.widget.LinearLayout
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.booruapi.objects.Post
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Resource
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.views.mediaview.MediaView
import de.yochyo.yummybooru.utils.ManagerWrapper
import de.yochyo.yummybooru.utils.general.*
import de.yochyo.yummybooru.utils.network.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PictureAdapter(val activity: PictureActivity, val viewPager: ViewPager, val m: ManagerWrapper) : PagerAdapter() {
    private val db = activity.db

    private var size = m.posts.size
    fun updatePosts(posts: Collection<Post>) {
        size = m.posts.size
        notifyDataSetChanged()
    }

    fun loadPreview(position: Int) {
        fun downloadPreview(index: Int) {
            if (index in m.posts.indices) {
                val p = m.posts.elementAt(position)
                download(activity, p.filePreviewURL, activity.preview(p.id), {}, cacheFile = true)
            }
        }
        downloadPreview(position - 1)
        downloadPreview(position + 1)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
    override fun getCount(): Int = size
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (position + 3 >= m.posts.size - 1) GlobalScope.launch { m.downloadNextPage() }
        val post = m.posts.elementAt(position)
        val view = createView(post, position)
        loadPreview(position)

        container.addView(view)
        view.tag = position
        return view
    }

    private fun createView(post: Post, position: Int): View {
        var lastSwipeUp = 0L
        fun onFlingUp(): Boolean {
            val time = System.currentTimeMillis()
            val p = m.posts.elementAt(position)
            if (time - lastSwipeUp > 400L) { //download
                downloadImage(activity, p)
                val snack = Snackbar.make(viewPager, activity.getString(R.string.download), Snackbar.LENGTH_SHORT)
                snack.show()
                GlobalScope.launch(Dispatchers.Main) {
                    delay(150)
                    snack.dismiss()
                }
            } else { //double swipe
                GlobalScope.launch {
                    for (tag in p.tags.filter { it.type == Tag.ARTIST })
                        db.tags += tag.toBooruTag(activity)
                }
            }
            lastSwipeUp = time
            return true
        }

        fun onFlipDown(): Boolean {
            activity.finish()
            return true
        }

        fun onClick() {
            if (db.clickToMoveToNextPicture)
                viewPager.currentItem = position + 1
        }

        fun forImage(): View {
            val view = PhotoView(activity)
            view.setAllowParentInterceptOnEdge(true)
            view.setOnSingleFlingListener { e1, e2, velocityX, velocityY ->
                when (GestureListener.getDirection(e1, e2)) {
                    GestureListener.Direction.DOWN -> onFlipDown()
                    GestureListener.Direction.UP -> onFlingUp()
                    else -> false
                }
            }
            view.setOnClickListener { onClick() }
            GlobalScope.launch {
                try {
                    var downloadedOriginalImage = false
                    download(activity, post.fileSampleURL, activity.sample(post.id), {
                        GlobalScope.launch(Dispatchers.Main) {
                            synchronized(downloadedOriginalImage) { downloadedOriginalImage = true }
                            it.loadIntoImageView(view)
                        }
                    }, downloadFirst = true, cacheFile = true)
                    download(
                        activity, post.filePreviewURL, activity.preview(post.id), {
                            GlobalScope.launch(Dispatchers.Main) {
                                synchronized(downloadedOriginalImage) {
                                    if (!downloadedOriginalImage) it.loadIntoImageView(view)
                                }
                            }
                        }, downloadFirst = true, cacheFile = true
                    )

                    val preview = activity.cache.getCachedFile(activity.preview(post.id))
                    if (preview != null) launch(Dispatchers.Main) { preview.loadIntoImageView(view) }

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return view
        }

        fun forVideo(): View {
            val layout = LinearLayout(activity)
            layout.gravity = Gravity.CENTER
            layout.orientation = LinearLayout.HORIZONTAL
            layout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            val view = MediaView(activity)
            layout.addView(view)
            view.setVideoPath(post.fileSampleURL)
            if (m.position == position) view.resume() //video wouldn't start if the first (the visible one after starting the activity) post is a video

            val detector = GestureDetector(activity, object : GestureListener() {
                override fun onSwipe(direction: Direction): Boolean {
                    return when (direction) {
                        Direction.UP -> onFlingUp()
                        Direction.DOWN -> onFlipDown()
                        else -> false
                    }
                }

                override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                    onClick()
                    return true
                }
            })
            layout.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
            return layout
        }

        return when (Resource.typeFromMimeType(post.fileSampleURL.mimeType ?: "")) {
            Resource.VIDEO -> forVideo()
            else -> forImage()
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        if (obj is LinearLayout) (obj.getChildAt(0) as MediaView).destroy()
        container.removeView(obj as View)
    }
}