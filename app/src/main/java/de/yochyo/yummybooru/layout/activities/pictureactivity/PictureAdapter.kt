package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import com.github.chrisbanes.photoview.OnSingleFlingListener
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.saveDownload
import de.yochyo.yummybooru.utils.Manager
import de.yochyo.yummybooru.utils.general.*
import de.yochyo.yummybooru.utils.network.download
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PictureAdapter(val activity: AppCompatActivity, val m: Manager) : PagerAdapter() {
    private val db = activity.db
    fun updatePosts() {
        notifyDataSetChanged()
        activity.view_pager.currentItem = m.position
    }

    private fun downloadOriginalPicture(p: Post) {
        GlobalScope.launch {
            saveDownload(activity, if (db.downloadOriginal) p.fileURL else p.fileSampleURL, if (db.downloadOriginal) activity.original(p.id) else activity.sample(p.id), p)
        }
    }

    fun loadPreview(position: Int) {
        fun downloadPreview(index: Int) {
            if (index in 0 until m.posts.size) {
                val p = m.posts[index]
                download(activity, p.filePreviewURL, activity.preview(p.id), {}, false, true)
            }
        }
        downloadPreview(position - 1)
        downloadPreview(position + 1)
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
    override fun getCount(): Int = m.posts.size
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (position + 3 >= m.posts.size - 1) GlobalScope.launch { m.downloadNextPage(activity) }
        if (position == m.posts.size - 1) {
            GlobalScope.launch {
                m.loadNextPage(activity)
            }
        }
        val imageView = activity.layoutInflater.inflate(R.layout.picture_item_view, container, false) as PhotoView
        imageView.setAllowParentInterceptOnEdge(true)
        imageView.setOnSingleFlingListener(object : OnSingleFlingListener {
            private var lastSwipeUp = 0L

            fun onFlingUp(position: Int) {
                val time = System.currentTimeMillis()
                val p = m.posts.elementAt(position)
                if (time - lastSwipeUp > 400L) { //download
                    downloadOriginalPicture(p)
                    val snack = Snackbar.make(activity.view_pager, activity.getString(R.string.download), Snackbar.LENGTH_SHORT)
                    snack.show()
                    GlobalScope.launch(Dispatchers.Main) {
                        delay(150)
                        snack.dismiss()
                    }
                } else { //add to history
                    GlobalScope.launch {
                        for (tag in p.getTags().filter { it.type == Tag.ARTIST })
                            db.addTag(tag)
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
        val p = m.posts.elementAt(position)
        loadPreview(position)
        GlobalScope.launch {
            try {
                val preview = activity.cache.getCachedFile(activity.preview(p.id))
                if (preview != null) launch(Dispatchers.Main) { preview.loadInto(imageView) }
                download(activity, p.fileSampleURL, activity.sample(p.id), { GlobalScope.launch(Dispatchers.Main) { it.loadInto(imageView) } }, downloadFirst = true, cacheFile = true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        container.addView(imageView)
        return imageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) = container.removeView(`object` as View)
}