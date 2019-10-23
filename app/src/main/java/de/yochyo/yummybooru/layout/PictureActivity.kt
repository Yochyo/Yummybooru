package de.yochyo.yummybooru.layout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.github.chrisbanes.photoview.OnSingleFlingListener
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.downloads.LoadManagerPageEvent
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.api.downloads.cache
import de.yochyo.yummybooru.api.downloads.downloadImage
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.res.Menus
import de.yochyo.yummybooru.utils.*
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.*


class PictureActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context, manager: Manager) {
            Manager.current = manager
            context.startActivity(Intent(context, PictureActivity::class.java))
        }
    }

    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var adapter: PageAdapter
    private val currentTags = ArrayList<Tag>()
    lateinit var managerListener: Listener<LoadManagerPageEvent>
    lateinit var m: Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        setSupportActionBar(toolbar_picture)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        m = Manager.current
        nav_view_picture.bringToFront()

        tagRecyclerView = nav_view_picture.findViewById(R.id.recycle_view_info)
        tagRecyclerView.adapter = InfoAdapter()
        tagRecyclerView.layoutManager = LinearLayoutManager(this)
        with(view_pager) {
            adapter = PageAdapter().apply { this@PictureActivity.adapter = this }

            managerListener = m.loadManagerPageEvent.registerListener { this@PictureActivity.adapter.updatePosts() }
            this@PictureActivity.adapter.updatePosts()
            m.currentPost?.updateCurrentTags(m.position)

            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(position: Int, offset: Float, p2: Int) {
                    if (offset == 0.0F && m.position != position) {
                        m.position = position
                        m.currentPost?.updateCurrentTags(position)
                    }
                }

                override fun onPageScrollStateChanged(p0: Int) {}
                override fun onPageSelected(position: Int) {}
            })
        }
    }

    private fun Post.updateCurrentTags(wasCurrentPosition: Int) {
        supportActionBar?.title = id.toString()

        currentTags.clear()
        tagRecyclerView.adapter?.notifyDataSetChanged()
        GlobalScope.launch {
            val tags = getTags()
            launch(Dispatchers.Main) {
                if (wasCurrentPosition == m.position) {
                    currentTags += tags
                    tagRecyclerView.adapter?.notifyDataSetChanged()
                    tagRecyclerView.scrollToPosition(0)
                }
            }
        }
    }

    private fun downloadOriginalPicture(p: Post) {
        GlobalScope.launch {
            if (db.downloadOriginal) FileUtils.writeOrDownloadFile(this@PictureActivity, p, original(p.id), p.fileURL, Server.currentServer)
            else FileUtils.writeOrDownloadFile(this@PictureActivity, p, sample(p.id), p.fileSampleURL, Server.currentServer)
        }
    }

    private inner class PageAdapter : PagerAdapter() {
        fun updatePosts() {
            notifyDataSetChanged()
            view_pager.currentItem = m.position
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
        override fun getCount(): Int = m.posts.size
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            if (position + 3 >= m.posts.size - 1) GlobalScope.launch { m.downloadNextPage(this@PictureActivity) }
            if (position == m.posts.size - 1) {
                GlobalScope.launch {
                    val posts = m.loadNextPage(this@PictureActivity)
                    if (posts != null)
                        for (p in posts) downloadImage(p.filePreviewURL, preview(p.id), {}, downloadNow = false)
                }
            }
            val imageView = layoutInflater.inflate(R.layout.picture_item_view, container, false) as PhotoView
            imageView.setAllowParentInterceptOnEdge(true)
            imageView.setOnSingleFlingListener(object : OnSingleFlingListener {
                private var lastSwipeUp = 0L
                override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    when (Fling.getDirection(e1, e2)) {
                        Fling.Direction.DOWN -> finish()
                        Fling.Direction.UP -> {
                            val time = System.currentTimeMillis()
                            val p = m.posts.elementAt(position)
                            if (time - lastSwipeUp > 400L) { //download
                                downloadOriginalPicture(p)
                                val snack = Snackbar.make(view_pager, getString(R.string.download), Snackbar.LENGTH_SHORT)
                                snack.show()
                                GlobalScope.launch(Dispatchers.Main) {
                                    delay(150)
                                    snack.dismiss()
                                }
                            } else { //add to history
                                GlobalScope.launch {
                                    for (tag in p.getTags().filter { it.type == Tag.ARTIST })
                                        db.addTag(this@PictureActivity, tag)
                                }
                            }
                            lastSwipeUp = time
                        }
                        else -> return false
                    }
                    return true
                }
            })
            val p = m.posts.elementAt(position)
            GlobalScope.launch {
                try {
                    val preview = cache.getCachedFile(preview(p.id))
                    if (preview != null) launch(Dispatchers.Main) { preview.loadInto(imageView) }
                    downloadImage(p.fileSampleURL, sample(p.id), { it.loadInto(imageView) })
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            container.addView(imageView)
            return imageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) = container.removeView(`object` as View)
    }

    private inner class InfoAdapter : RecyclerView.Adapter<InfoButtonHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoButtonHolder = InfoButtonHolder(layoutInflater.inflate(R.layout.info_item_button, parent, false) as Toolbar).apply {
            toolbar.inflateMenu(R.menu.picture_info_menu)
            toolbar.setOnClickListener {
                PreviewActivity.startActivity(this@PictureActivity, toolbar.findViewById<TextView>(R.id.info_textview).text.toString())
                drawer_picture.closeDrawer(GravityCompat.END)
            }
            toolbar.setOnMenuItemClickListener {
                val tag = currentTags[adapterPosition]
                when (it.itemId) {
                    R.id.picture_info_item_add_history -> GlobalScope.launch { db.addTag(this@PictureActivity, Tag(tag.name, tag.type)) }
                    R.id.picture_info_item_add_favorite -> {
                        if (db.getTag(tag.name) == null) GlobalScope.launch { db.addTag(this@PictureActivity, Tag(tag.name, tag.type, true)) }
                        else GlobalScope.launch { db.changeTag(this@PictureActivity, tag.copy(isFavorite = true)) }
                    }
                    R.id.picture_info_item_subscribe -> {
                        if (db.getSubscription(tag.name) == null) GlobalScope.launch { db.addSubscription(this@PictureActivity, Subscription.fromTag(tag)) }
                        else GlobalScope.launch { db.deleteSubscription(this@PictureActivity, tag.name) }
                    }
                }
                notifyItemChanged(adapterPosition)
                drawer_picture.closeDrawer(GravityCompat.END)
                true
            }
        }

        override fun onBindViewHolder(holder: InfoButtonHolder, position: Int) {
            val tag = currentTags[position]
            val textView = holder.toolbar.findViewById<TextView>(R.id.info_textview)
            textView.text = tag.name
            textView.setColor(tag.color)
            Menus.initPictureInfoTagMenu(holder.toolbar.menu, tag)
        }

        override fun getItemCount(): Int = currentTags.size
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.picture_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.show_info -> drawer_picture.openDrawer(GravityCompat.END)
            R.id.save -> m.currentPost?.apply { downloadOriginalPicture(this) }
            R.id.share -> {
                val post = m.currentPost
                if (post != null) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, post.fileURL)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(intent, null))
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        m.loadManagerPageEvent.removeListener(managerListener)
        super.onDestroy()
    }

    private inner class InfoButtonHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}

