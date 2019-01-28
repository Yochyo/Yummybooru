package de.yochyo.yBooru.layout

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
import de.yochyo.yBooru.R
import de.yochyo.yBooru.api.Api
import de.yochyo.yBooru.api.Tag
import de.yochyo.yBooru.file.FileManager
import de.yochyo.yBooru.manager.Manager
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PictureActivity : AppCompatActivity() {
    private var currentTags = ArrayList<Tag>()
    private lateinit var recycleView: RecyclerView
    lateinit var m: Manager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        setSupportActionBar(toolbar_picture)

        m = Manager.get(intent.getStringExtra("tags"))
        nav_view_picture.bringToFront()
        recycleView = nav_view_picture.getHeaderView(0).findViewById(R.id.recycle_view_info)
        recycleView.adapter = InfoAdapter()
        recycleView.layoutManager = LinearLayoutManager(this)
        with(view_pager) {
            adapter = PageAdapter()
            currentItem = m.position
            val p = m.currentPost
            if (p != null) {
                currentTags.apply { clear();addAll(p.tagsCopyright);addAll(p.tagsArtist); addAll(p.tagsCharacter); addAll(p.tagsGeneral); addAll(p.tagsMeta) }
                recycleView.adapter?.notifyDataSetChanged()
            }
            addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(p0: Int) {}
                override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {}
                override fun onPageSelected(p0: Int) {
                    val post = m.dataSet[p0]
                    if (post != null) {
                        currentTags.apply { clear();addAll(post.tagsCopyright);addAll(post.tagsArtist); addAll(post.tagsCharacter); addAll(post.tagsGeneral); addAll(post.tagsMeta) }
                        recycleView.adapter?.notifyDataSetChanged()
                    }
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
                    GlobalScope.launch { FileManager.writeFile(p, Api.downloadImage(this@PictureActivity, p.fileLargeURL, "${p.id}Large")); launch(Dispatchers.Main) { Toast.makeText(this@PictureActivity, "Download finished", Toast.LENGTH_SHORT).show() } }
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.picture_menu, menu)
        return true
    }

    private inner class PageAdapter : PagerAdapter() {
        override fun isViewFromObject(p0: View, p1: Any): Boolean = p0 == p1
        override fun getCount(): Int = m.dataSet.size
        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val imageView = LayoutInflater.from(this@PictureActivity).inflate(R.layout.picture_item_view, container, false) as ImageView
            val post = m.dataSet[position]
            if (post != null) {
                GlobalScope.launch {
                    val preview = Api.downloadImage(this@PictureActivity, post.filePreviewURL, "${post.id}Preview")
                    launch(Dispatchers.Main) { imageView.setImageBitmap(preview) }
                    launch {
                        val bitmap = Api.downloadImage(this@PictureActivity, post.fileLargeURL, "${post.id}Large")
                        launch(Dispatchers.Main) { imageView.setImageBitmap(bitmap) }
                    }
                }
                container.addView(imageView)
            }
            return imageView
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            //TODO was soll ich machen
        }

    }

    private inner class InfoAdapter : RecyclerView.Adapter<InfoButtonHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoButtonHolder = InfoButtonHolder(LayoutInflater.from(parent.context).inflate(R.layout.info_item_button, parent, false) as Button)

        override fun getItemCount(): Int = currentTags.size

        override fun onBindViewHolder(holder: InfoButtonHolder, position: Int) {
            holder.button.text = currentTags[position].name
            if (Build.VERSION.SDK_INT > 22)
                holder.button.setTextColor(getColor(currentTags[position].color))
            else
                holder.button.setTextColor(resources.getColor(currentTags[position].color))
        }
    }

    private inner class InfoButtonHolder(val button: Button) : RecyclerView.ViewHolder(button)
}