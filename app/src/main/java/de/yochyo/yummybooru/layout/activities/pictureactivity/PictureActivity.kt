package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.saveDownload
import de.yochyo.yummybooru.utils.general.currentManager
import de.yochyo.yummybooru.utils.general.original
import de.yochyo.yummybooru.utils.general.sample
import de.yochyo.yummybooru.utils.manager.ManagerWrapper
import de.yochyo.yummybooru.utils.manager.OnDownloadPageEvent
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PictureActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context, manager: ManagerWrapper) {
            currentManager = manager
            context.startActivity(Intent(context, PictureActivity::class.java))
        }
    }

    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var pictureAdapter: PictureAdapter
    private lateinit var tagInfoAdapter: TagInfoAdapter

    private val managerListener = Listener.create<OnDownloadPageEvent> { this@PictureActivity.pictureAdapter.updatePosts() }
    lateinit var m: ManagerWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        setSupportActionBar(toolbar_picture)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        m = currentManager
        nav_view_picture.bringToFront()

        tagRecyclerView = nav_view_picture.findViewById(R.id.recycle_view_info)
        tagInfoAdapter = TagInfoAdapter(this).apply { tagRecyclerView.adapter = this }
        tagRecyclerView.layoutManager = LinearLayoutManager(this)
        with(view_pager) {
            adapter = PictureAdapter(this@PictureActivity, m).apply { this@PictureActivity.pictureAdapter = this }
            m.onDownloadPageEvent.registerListener(managerListener)
            this@PictureActivity.pictureAdapter.updatePosts()
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

        tagInfoAdapter.updateInfoTags(emptyList())
        GlobalScope.launch {
            val tags = getTags()
            launch(Dispatchers.Main) {
                if (wasCurrentPosition == m.position) {
                    tagInfoAdapter.updateInfoTags(tags)
                    tagRecyclerView.scrollToPosition(0)
                }
            }
        }
    }

    private fun downloadOriginalPicture(p: Post) {
        GlobalScope.launch {
            saveDownload(this@PictureActivity, if (db.downloadOriginal) p.fileURL else p.fileSampleURL, if (db.downloadOriginal) original(p.id) else sample(p.id), p)
        }
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
        m.onDownloadPageEvent.removeListener(managerListener)
        super.onDestroy()
    }

}

