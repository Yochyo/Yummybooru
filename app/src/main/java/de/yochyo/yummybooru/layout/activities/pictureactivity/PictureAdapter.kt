package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.view.GestureDetector
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
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
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PictureAdapter(val activity: AppCompatActivity, val m: ManagerWrapper) : PagerAdapter() {
    private val db = activity.db
    fun updatePosts() {
        notifyDataSetChanged()
    }

    fun loadPreview(position: Int) {
        fun downloadPreview(index: Int) {
            if (index in 0 until m.posts.size) {
                val p = m.posts[index]
                download(activity, p.filePreviewURL, activity.preview(p.id), {}, cacheFile = true)
            }
        }
        downloadPreview(position - 1)
        downloadPreview(position + 1)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
    override fun getCount(): Int = m.posts.size
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (position + 3 >= m.posts.size - 1) GlobalScope.launch { m.downloadNextPage() }
        /*
        val imageView = activity.layoutInflater.inflate(R.layout.picture_item_view, container, false) as PhotoView
        imageView.setAllowParentInterceptOnEdge(true)
        imageView.setOnSingleFlingListener(object : OnSingleFlingListener {
            private var lastSwipeUp = 0L

            fun onFlingUp(position: Int) {
                val time = System.currentTimeMillis()
                val p = m.posts.elementAt(position)
                if (time - lastSwipeUp > 400L) { //download
                    downloadImage(activity, p)
                    val snack = Snackbar.make(activity.view_pager, activity.getString(R.string.download), Snackbar.LENGTH_SHORT)
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
            }

            override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                when (Fling.getDirection(e1, e2)) {
                    Fling.Direction.DOWN -> activity.finish()
                    Fling.Direction.UP -> onFlingUp(position)
                    else -> return false
                }
                return true
            }
        })
         */
        val post = m.posts.elementAt(position)
        val view = createView(post, position)
        loadPreview(position)

        container.addView(view)
        return view
    }

    private fun createView(post: Post, position: Int): View {
        fun forImage(): View {
            val view = PhotoView(activity)
            view.setAllowParentInterceptOnEdge(true)
            GlobalScope.launch {
                try {
                    val preview = activity.cache.getCachedFile(activity.preview(post.id))
                    if (preview != null) launch(Dispatchers.Main) { preview.loadIntoImageView(view) }
                    download(activity, post.fileSampleURL, activity.sample(post.id), { GlobalScope.launch(Dispatchers.Main) { it.loadIntoImageView(view) } }, downloadFirst = true, cacheFile = true)
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
            layout.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT)
            val view = MediaView(activity)
            layout.addView(view)
            view.setVideoPath(post.fileSampleURL)
            return layout
        }

        var lastSwipeUp = 0L

        val detector = GestureDetector(activity, object : GestureListener() {
            override fun onSwipe(direction: Direction): Boolean {
                return if (direction == Direction.Up) {
                    val time = System.currentTimeMillis()
                    val p = m.posts.elementAt(position)
                    if (time - lastSwipeUp > 400L) { //download
                        downloadImage(activity, p)
                        val snack = Snackbar.make(activity.view_pager, activity.getString(R.string.download), Snackbar.LENGTH_SHORT)
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
                    true
                } else if (direction == Direction.Down) {
                    activity.finish()
                    true
                } else false
            }
        })

        val view = when (Resource.typeFromMimeType(post.fileSampleURL.mimeType ?: "")) {
            Resource.VIDEO -> forVideo()
            else -> forImage()
        }
        view.setOnTouchListener { _, event -> detector.onTouchEvent(event) }
        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        if (obj is LinearLayout) (obj.getChildAt(0) as MediaView).destroy()
        container.removeView(obj as View)
    }
}