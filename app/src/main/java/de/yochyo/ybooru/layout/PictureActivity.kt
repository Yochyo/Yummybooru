package de.yochyo.ybooru.layout

import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import de.yochyo.ybooru.api.api.Api
import de.yochyo.ybooru.api.downloadImage
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.database.entities.Subscription
import de.yochyo.ybooru.database.entities.Tag
import de.yochyo.ybooru.layout.res.Menus
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.*
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PictureActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context, tags: String) = context.startActivity(Intent(context, PictureActivity::class.java).apply { putExtra("tags", tags) })
    }

    private lateinit var recycleView: RecyclerView
    private var currentTags = ArrayList<Tag>()
    lateinit var m: Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        setSupportActionBar(toolbar_picture)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        m = Manager.get(intent.getStringExtra("tags"))
        supportActionBar?.title = m.currentPost?.id.toString()
        nav_view_picture.bringToFront()
        recycleView = nav_view_picture.findViewById(R.id.recycle_view_info)
        recycleView.adapter = InfoAdapter()
        recycleView.layoutManager = LinearLayoutManager(this)

        with(view_pager) {
            adapter = PageAdapter()
            currentItem = m.position
            val p = m.currentPost
            if (p != null) {
                val pos = m.position
                GlobalScope.launch {
                    val tags = p.tags as ArrayList<Tag>
                    launch(Dispatchers.Main) {
                        if (pos == m.position) {
                            currentTags = tags
                            recycleView.adapter?.notifyDataSetChanged()
                        }
                    }
                }
            }

            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(position: Int, offset: Float, p2: Int) {
                    if (offset == 0.0F && m.position != position) {
                        m.position = position

                        val post = m.currentPost
                        if (post != null) {
                            supportActionBar?.title = post.id.toString()
                            currentTags.clear()
                            recycleView.adapter?.notifyDataSetChanged()
                            GlobalScope.launch {
                                val tags = post.tags as ArrayList<Tag>
                                launch(Dispatchers.Main) {
                                    if (position == m.position) {
                                        currentTags = tags
                                        recycleView.adapter?.notifyDataSetChanged()
                                    }
                                }
                            }
                        }
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

    fun loadNextPage(page: Int) {
        GlobalScope.launch {
            m.downloadPage(page)
            launch(Dispatchers.Main) {
                m.loadPage(page)
                view_pager.adapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun downloadOriginalPicture(p: Post) {
        GlobalScope.launch(Dispatchers.IO) {
            FileUtils.writeOrDownloadFile(this@PictureActivity, p, original(p.id), p.fileURL) {
                Toast.makeText(this@PictureActivity, "Downloaded ${p.id}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private inner class PageAdapter : PagerAdapter() {
        override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
        override fun getCount(): Int = m.dataSet.size
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            if (position + 3 >= m.dataSet.lastIndex) GlobalScope.launch { m.downloadPage(m.currentPage + 1) }
            if (position == m.dataSet.lastIndex) loadNextPage(m.currentPage + 1)
            val imageView = LayoutInflater.from(this@PictureActivity).inflate(R.layout.picture_item_view, container, false) as PhotoView
            imageView.setAllowParentInterceptOnEdge(true)
            imageView.setOnSingleFlingListener(object : OnSingleFlingListener {
                override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                    when (Fling.getDirection(e1, e2)) {
                        Fling.Direction.down -> finish()
                        Fling.Direction.up -> {
                            val p = m.dataSet[position]
                            downloadOriginalPicture(p)
                        }
                        else -> return false
                    }
                    return true
                }
            })
            val p = m.dataSet[position]
            downloadImage(p.filePreviewURL, preview(p.id), {
                imageView.setImageBitmap(it)
                downloadImage(p.fileURL, original(p.id), { imageView.setImageBitmap(it) }, true)
            }, false)

            container.addView(imageView)
            return imageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) = container.removeView(`object` as View)
    }

    private inner class InfoAdapter : RecyclerView.Adapter<InfoButtonHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoButtonHolder = InfoButtonHolder(LayoutInflater.from(parent.context).inflate(R.layout.info_item_button, parent, false) as Toolbar).apply {
            toolbar.inflateMenu(R.menu.picture_info_menu)
            toolbar.setOnClickListener {
                PreviewActivity.startActivity(this@PictureActivity, toolbar.findViewById<TextView>(R.id.info_textview).text.toString())
                drawer_picture.closeDrawer(GravityCompat.END)
            }
            toolbar.setOnMenuItemClickListener {
                val tag = currentTags[adapterPosition]
                when (it.itemId) {
                    R.id.picture_info_item_add_history -> {
                        db.addTag(Tag(tag.name, tag.type))
                        Toast.makeText(this@PictureActivity, "Add tag ${tag.name}", Toast.LENGTH_SHORT).show()
                    }
                    R.id.picture_info_item_add_favorite -> {
                        if (db.getTag(tag.name) == null) db.addTag(Tag(tag.name, tag.type, true))
                        else db.changeTag(tag.apply { isFavorite = true })
                        Toast.makeText(this@PictureActivity, "Add favorite ${tag.name}", Toast.LENGTH_SHORT).show()
                    }
                    R.id.picture_info_item_subscribe -> {
                        if (db.getSubscription(tag.name) == null) {
                            db.addTag(Tag(tag.name, tag.type, tag.isFavorite))
                            GlobalScope.launch { val currentID = Api.newestID();db.addSubscription(Subscription(tag.name, tag.type, currentID)) }
                            Toast.makeText(this@PictureActivity, "Add subscription${tag.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            db.deleteSubscription(tag.name)
                            Toast.makeText(this@PictureActivity, "Unsubscribed ${tag.name}", Toast.LENGTH_SHORT).show()
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

