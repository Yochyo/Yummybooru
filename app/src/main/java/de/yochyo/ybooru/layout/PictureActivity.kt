package de.yochyo.ybooru.layout

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toolbar
import de.yochyo.ybooru.GestureListener
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Tag
import de.yochyo.ybooru.api.downloadImage
import de.yochyo.ybooru.database
import de.yochyo.ybooru.file.FileManager
import de.yochyo.ybooru.layout.res.Menus
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.large
import de.yochyo.ybooru.utils.preview
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PictureActivity : AppCompatActivity() {
    private var currentPage: Int = 1
    private lateinit var tags: Array<out String>

    companion object {
        fun startActivity(context: Context, tags: String) {
            context.startActivity(Intent(context, PictureActivity::class.java).apply { putExtra("tags", tags) })
        }
    }

    private var currentTags = ArrayList<Tag>()
    private lateinit var recycleView: RecyclerView
    lateinit var m: Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        setSupportActionBar(toolbar_picture)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        m = Manager.get(intent.getStringExtra("tags").apply { tags = this.split(" ").toTypedArray() })
        currentPage = m.currentPage
        nav_view_picture.bringToFront()

        initRecycleView()
        with(view_pager) {
            adapter = PageAdapter()
            currentItem = m.position
            offscreenPageLimit = 2
            val p = m.currentPost
            if (p != null) {
                currentTags.apply { clear();addAll(p.tagsCopyright);addAll(p.tagsArtist); addAll(p.tagsCharacter); addAll(p.tagsGeneral); addAll(p.tagsMeta) }
                recycleView.adapter?.notifyDataSetChanged()
            }


            val detector = GestureDetector(this@PictureActivity, object : GestureListener() {
                override fun onSwipe(direction: Direction): Boolean {
                    when (direction) {
                        Direction.down -> finish()
                        else -> return false
                    }
                    return true
                }
            })
            setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    return detector.onTouchEvent(event)
                }
            })


            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(p0: Int) {}
                override fun onPageScrolled(position: Int, offset: Float, p2: Int) {
                    //TODO hier optimieren falls switchen der seiten laggt
                    if (offset == 0.0F && m.position != position) {
                        m.position = position
                        val post = m.dataSet[position]
                        currentTags.apply { clear();addAll(post.tagsCopyright);addAll(post.tagsArtist); addAll(post.tagsCharacter); addAll(post.tagsGeneral); addAll(post.tagsMeta) }
                        recycleView.adapter?.notifyDataSetChanged()
                    }
                }

                override fun onPageSelected(position: Int) {}
            })

        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish();return true
            }
            R.id.show_info -> {
                drawer_picture.openDrawer(GravityCompat.END)
                return true
            }
            R.id.save -> {
                val p = m.currentPost
                if (p != null)
                    downloadImage(p.fileLargeURL, large(p.id), { launch { FileManager.writeFile(p, it); Toast.makeText(this@PictureActivity, "Download finished", Toast.LENGTH_SHORT).show() } }, true)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.picture_menu, menu)
        return true
    }

    private fun initRecycleView() {
        recycleView = nav_view_picture.getHeaderView(0).findViewById(R.id.recycle_view_info)
        recycleView.adapter = InfoAdapter()
        recycleView.layoutManager = LinearLayoutManager(this)
    }

    fun loadNextPage(page: Int) {
        if (currentPage - 1 == m.currentPage)
            GlobalScope.launch {
                m.getOrDownloadPage(this@PictureActivity, page)
                launch(Dispatchers.Main) {
                    m.getAndInitPage(this@PictureActivity, page)
                    view_pager.adapter!!.notifyDataSetChanged()
                }
            }
    }

    fun preloadNextPage(page: Int) {
        if (currentPage == m.currentPage) {
            currentPage++
            GlobalScope.launch {
                m.getOrDownloadPage(this@PictureActivity, page)
            }
        }
    }

    private inner class PageAdapter : PagerAdapter() {
        override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
        override fun getCount(): Int = m.dataSet.size
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            if (position + 3 >= m.dataSet.lastIndex)
                preloadNextPage(m.currentPage + 1)
            if (position == m.dataSet.lastIndex)
                loadNextPage(m.currentPage + 1)
            val imageView = LayoutInflater.from(this@PictureActivity).inflate(R.layout.picture_item_view, container, false) as ImageView
            val p = m.dataSet[position]

            downloadImage(p.filePreviewURL, preview(p.id), {
                imageView.setImageBitmap(it)
                downloadImage(p.fileLargeURL, large(p.id), { imageView.setImageBitmap(it) }, true)
            }, true)

            container.addView(imageView)
            return imageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }

    private inner class InfoAdapter : RecyclerView.Adapter<InfoButtonHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoButtonHolder = InfoButtonHolder(LayoutInflater.from(parent.context).inflate(R.layout.info_item_button, parent, false) as Toolbar).apply {
            toolbar.setOnClickListener {
                PreviewActivity.startActivity(this@PictureActivity, toolbar.findViewById<TextView>(R.id.info_textview).text.toString())
                drawer_picture.closeDrawer(GravityCompat.END)
            }
            toolbar.inflateMenu(R.menu.picture_info_menu)
            toolbar.setOnMenuItemClickListener {
                val tag = currentTags[adapterPosition]
                when (it.itemId) {
                    R.id.picture_info_item_add_history -> {
                        database.addTag(tag.name, tag.type, false)
                        Toast.makeText(this@PictureActivity, "Add tag ${tag.name}", Toast.LENGTH_SHORT).show()
                    }
                    R.id.picture_info_item_add_favorite -> {
                        val tagInDatabase = database.getTag(tag.name)
                        if (tagInDatabase == null) database.addTag(tag.name, tag.type, true)
                        else database.changeTag(tag.apply { isFavorite = true })
                        Toast.makeText(this@PictureActivity, "Add favorite ${tag.name}", Toast.LENGTH_SHORT).show()
                    }
                    R.id.picture_info_item_subscribe -> {
                        if (database.getSubscription(tag.name) == null) {
                            database.addSubscription(tag.name, 0)
                            Toast.makeText(this@PictureActivity, "Subscribed ${tag.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            database.removeSubscription(tag.name)
                            Toast.makeText(this@PictureActivity, "Unsubscribed ${tag.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            }
        }

        override fun getItemCount(): Int = currentTags.size
        override fun onBindViewHolder(holder: InfoButtonHolder, position: Int) {
            val tag = currentTags[position]
            Menus.initPictureInfoTagMenu(this@PictureActivity, holder.toolbar.menu, tag)
            val textView = holder.toolbar.findViewById<TextView>(R.id.info_textview)
            textView.text = tag.name
            if (Build.VERSION.SDK_INT > 22) textView.setTextColor(getColor(tag.color))
            else textView.setTextColor(resources.getColor(tag.color))
        }
    }

    private inner class InfoButtonHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}

