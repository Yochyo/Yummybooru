package de.yochyo.ybooru.layout

import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.view.GravityCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import com.github.chrisbanes.photoview.OnSingleFlingListener
import com.github.chrisbanes.photoview.PhotoView
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.api.entities.Subscription
import de.yochyo.ybooru.api.entities.Tag
import de.yochyo.ybooru.api.downloads.Manager
import de.yochyo.ybooru.api.downloads.cache
import de.yochyo.ybooru.api.downloads.downloadImage
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.layout.res.Menus
import de.yochyo.ybooru.utils.*
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class PictureActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context, tags: String) = context.startActivity(Intent(context, PictureActivity::class.java).apply { putExtra("tags", tags) })
    }

    private val observer = Observer<ArrayList<Post>> { if (it != null) adapter.updatePosts(it) }
    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var adapter: PageAdapter
    private val currentTags = ArrayList<Tag>()
    lateinit var m: Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        setSupportActionBar(toolbar_picture)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        m = Manager.get(intent.getStringExtra("tags"))
        nav_view_picture.bringToFront()

        tagRecyclerView = nav_view_picture.findViewById(R.id.recycle_view_info)
        tagRecyclerView.adapter = InfoAdapter()
        tagRecyclerView.layoutManager = LinearLayoutManager(this)

        adapter = PageAdapter()
        with(view_pager) {
            adapter = this@PictureActivity.adapter
            m.posts.observe(this@PictureActivity, observer)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.show_info -> drawer_picture.openDrawer(GravityCompat.END)
            R.id.save -> m.currentPost?.apply { downloadOriginalPicture(this) }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.picture_menu, menu)
        return true
    }

    override fun onDestroy() {
        m.posts.removeObserver(observer)
        super.onDestroy()
    }

    fun loadNextPage(page: Int) {
        GlobalScope.launch {
            val postList = m.downloadPage(page)
            launch(Dispatchers.Main) {
                m.loadPage(page)
            }
        }
    }

    private fun Post.updateCurrentTags(wasCurrentPosition: Int) {
        supportActionBar?.title = id.toString()

        currentTags.clear()
        tagRecyclerView.adapter?.notifyDataSetChanged()
        GlobalScope.launch {
            val tags = getTags() as ArrayList<Tag>
            launch(Dispatchers.Main) {
                if (wasCurrentPosition == m.position) {
                    currentTags += tags
                    tagRecyclerView.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    private fun downloadOriginalPicture(p: Post) {
        GlobalScope.launch {
            if (db.downloadOriginal) {
                FileUtils.writeOrDownloadFile(this@PictureActivity, p, original(p.id), p.fileURL) {
                    Toast.makeText(this@PictureActivity, "${getString(R.string.downloaded)}: ${p.id}", Toast.LENGTH_SHORT).show()
                }
            } else {
                FileUtils.writeOrDownloadFile(this@PictureActivity, p, sample(p.id), p.fileSampleURL) {
                    Toast.makeText(this@PictureActivity, "${getString(R.string.downloaded)}: ${p.id}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private inner class PageAdapter : PagerAdapter() {
        var posts = ArrayList<Post>()

        fun updatePosts(array: ArrayList<Post>) {
            posts = array
            notifyDataSetChanged()
            view_pager.currentItem = m.position
        }

        override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
        override fun getCount(): Int = posts.size
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            if (position + 3 >= posts.lastIndex) GlobalScope.launch {
                val postList = m.downloadPage(m.currentPage + 1)
                for (p in postList)
                    downloadImage(p.filePreviewURL, preview(p.id), {}, downloadNow = false)
            }
            if (position == posts.lastIndex) loadNextPage(m.currentPage + 1)
            val imageView = layoutInflater.inflate(R.layout.picture_item_view, container, false) as PhotoView
            imageView.setAllowParentInterceptOnEdge(true)
            imageView.setOnSingleFlingListener(object : OnSingleFlingListener {
                override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    when (Fling.getDirection(e1, e2)) {
                        Fling.Direction.down -> finish()
                        Fling.Direction.up -> {
                            val p = posts[position]
                            downloadOriginalPicture(p)
                            val snack = Snackbar.make(view_pager, getString(R.string.download), Snackbar.LENGTH_SHORT)
                            snack.show()
                            GlobalScope.launch(Dispatchers.Main) {
                                delay(150)
                                snack.dismiss()
                            }
                        }
                        else -> return false
                    }
                    return true
                }
            })
            val p = posts[position]
            GlobalScope.launch {
                val preview = cache.getCachedBitmap(preview(p.id))
                if (preview != null) launch(Dispatchers.Main) { imageView.setImageBitmap(preview) }
                downloadImage(p.fileSampleURL, sample(p.id), { imageView.setImageBitmap(it) })
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
                    R.id.picture_info_item_add_history -> {
                        GlobalScope.launch { db.addTag(Tag(tag.name, tag.type)) }
                        Toast.makeText(this@PictureActivity, "${getString(R.string.add_tag)} ${tag.name}", Toast.LENGTH_SHORT).show()
                    }
                    R.id.picture_info_item_add_favorite -> {
                        if (db.getTag(tag.name) == null) GlobalScope.launch { db.addTag(Tag(tag.name, tag.type, true)) }
                        else GlobalScope.launch { db.changeTag(tag.apply { isFavorite = true }) }
                        Toast.makeText(this@PictureActivity, "${getString(R.string.add_favorite)} ${tag.name}", Toast.LENGTH_SHORT).show()
                    }
                    R.id.picture_info_item_subscribe -> {
                        if (db.getSubscription(tag.name) == null) {
                            GlobalScope.launch { db.addSubscription(Subscription.fromTag(tag)) }
                            Toast.makeText(this@PictureActivity, "${getString(R.string.add_subscription)} ${tag.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            GlobalScope.launch { db.deleteSubscription(tag.name) }
                            Toast.makeText(this@PictureActivity, "${getString(R.string.unsubscribed)} ${tag.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                drawer_picture.closeDrawer(GravityCompat.END)
                true
            }
        }

        override fun getItemCount(): Int = currentTags.size
        override fun onBindViewHolder(holder: InfoButtonHolder, position: Int) {
            val tag = currentTags[position]
            val textView = holder.toolbar.findViewById<TextView>(R.id.info_textview)
            textView.text = tag.name
            textView.setColor(tag.color)
            Menus.initPictureInfoTagMenu(holder.toolbar.menu, tag)
        }
    }

    private inner class InfoButtonHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}

