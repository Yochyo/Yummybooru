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
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import de.yochyo.ybooru.GestureListener
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Api
import de.yochyo.ybooru.api.Tag
import de.yochyo.ybooru.file.FileManager
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.cache
import de.yochyo.ybooru.utils.large
import de.yochyo.ybooru.utils.preview
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PictureActivity : AppCompatActivity() {
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

        m = Manager.get(intent.getStringExtra("tags"))
        nav_view_picture.bringToFront()

        initRecycleView()
        with(view_pager) {
            adapter = PageAdapter()
            currentItem = m.position
            val p = m.currentPost
            if (p != null) {
                currentTags.apply { clear();addAll(p.tagsCopyright);addAll(p.tagsArtist); addAll(p.tagsCharacter); addAll(p.tagsGeneral); addAll(p.tagsMeta) }
                recycleView.adapter?.notifyDataSetChanged()
            }
            val detector = GestureDetector(this@PictureActivity, object : GestureListener() {
                override fun onSwipe(direction: Direction): Boolean {
                    when (direction) {
                        Direction.up -> println("UP")
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
                override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}
                override fun onPageSelected(position: Int) {
                    m.position = position
                    val post = m.dataSet[position]
                    currentTags.apply { clear();addAll(post.tagsCopyright);addAll(post.tagsArtist); addAll(post.tagsCharacter); addAll(post.tagsGeneral); addAll(post.tagsMeta) }
                    recycleView.adapter?.notifyDataSetChanged()
                }
            })
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.show_info -> {
                drawer_picture.openDrawer(GravityCompat.END)
                return true
            }
            R.id.save -> {
                val p = m.currentPost
                if (p != null)
                    GlobalScope.launch { FileManager.writeFile(p, Api.downloadImage(this@PictureActivity, p.fileLargeURL, large(p.id))); launch(Dispatchers.Main) { Toast.makeText(this@PictureActivity, "Download finished", Toast.LENGTH_SHORT).show() } }
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

    private inner class PageAdapter : PagerAdapter() {
        override fun isViewFromObject(view: View, `object`: Any): Boolean = view == `object`
        override fun getCount(): Int = m.dataSet.size
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val imageView = LayoutInflater.from(this@PictureActivity).inflate(R.layout.picture_item_view, container, false) as ImageView
            val post = m.dataSet[position]
            GlobalScope.launch {
                val preview = Api.downloadImage(this@PictureActivity, post.filePreviewURL, preview(post.id))
                launch(Dispatchers.Main) {
                    imageView.setImageBitmap(preview)
                    launch(Dispatchers.Default) {
                        val bitmap = Api.downloadImage(this@PictureActivity, post.fileLargeURL, large(post.id), false)
                        launch(Dispatchers.Main) { imageView.setImageBitmap(bitmap) }
                    }
                }
            }
            container.addView(imageView)
            return imageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            cache.removeBitmap(large(m.dataSet[position].id))
        }

    }

    private inner class InfoAdapter : RecyclerView.Adapter<InfoButtonHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoButtonHolder = InfoButtonHolder(LayoutInflater.from(parent.context).inflate(R.layout.info_item_button, parent, false) as Button).apply {
            button.setOnClickListener {
                println()
                PreviewActivity.startActivity(this@PictureActivity, button.text.toString())
                drawer_picture.closeDrawer(GravityCompat.END)
            }
        }

        override fun getItemCount(): Int = currentTags.size
        override fun onBindViewHolder(holder: InfoButtonHolder, position: Int) {
            val tag = currentTags[position]
            holder.button.text = tag.name
            if (Build.VERSION.SDK_INT > 22) holder.button.setTextColor(getColor(tag.color))
            else holder.button.setTextColor(resources.getColor(tag.color))
        }
    }

    private inner class InfoButtonHolder(val button: Button) : RecyclerView.ViewHolder(button)
}

