package de.yochyo.yBooru.layout

import android.os.Build
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.Button
import android.widget.Toast
import de.yochyo.yBooru.GestureListener
import de.yochyo.yBooru.R
import de.yochyo.yBooru.api.Api
import de.yochyo.yBooru.api.Post
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
        initGestureListener()

        m = Manager.get(intent.getStringExtra("tags"))
        nav_view_picture.bringToFront()
        recycleView = nav_view_picture.getHeaderView(0).findViewById(R.id.recycle_view_info)
        recycleView.adapter = Adapter()
        recycleView.layoutManager = LinearLayoutManager(this)
        val post = m.currentPost
        if (post != null) {
            loadImage(post)
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

    private fun loadImage(post: Post) {
        currentTags.apply { addAll(post.tagsCopyright);addAll(post.tagsArtist); addAll(post.tagsCharacter); addAll(post.tagsGeneral); addAll(post.tagsMeta) }
        recycleView.adapter?.notifyDataSetChanged()
        GlobalScope.launch {
            val preview = Api.downloadImage(this@PictureActivity, post.filePreviewURL, "${post.id}Preview")
            launch(Dispatchers.Main) { image_view.setImageBitmap(preview) }
            launch {
                val bitmap = Api.downloadImage(this@PictureActivity, post.fileLargeURL, "${post.id}Large")
                launch(Dispatchers.Main) { image_view.setImageBitmap(bitmap) }
            }
        }
    }

    private fun initGestureListener() {
        val gestureDetector = GestureDetector(this, object : GestureListener() {
            override fun onSwipe(direction: Direction): Boolean {
                if (direction == Direction.right) {
                    if (m.position >= 1) {
                        m.position -= 1
                        val post = m.currentPost
                        if (post != null)
                            loadImage(post)
                    }
                } else if (direction == Direction.left) {
                    if (m.position < m.dataSet.lastIndex) {
                        m.position += 1
                        val post = m.currentPost
                        if (post != null)
                            loadImage(post)
                    }
                }
                return false
            }
        })
        image_view.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View, event: MotionEvent?): Boolean {
                v.onTouchEvent(event)
                gestureDetector.onTouchEvent(event)
                return true
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.picture_menu, menu)
        return true
    }

    private inner class Adapter : RecyclerView.Adapter<InfoButtonHolder>() {
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