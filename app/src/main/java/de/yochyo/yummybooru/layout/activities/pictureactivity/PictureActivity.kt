package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import de.yochyo.booruapi.objects.Post
import de.yochyo.booruapi.objects.Tag
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.views.mediaview.MediaView
import de.yochyo.yummybooru.utils.ManagerWrapper
import de.yochyo.yummybooru.utils.general.downloadImage
import de.yochyo.yummybooru.utils.general.getCurrentManager
import de.yochyo.yummybooru.utils.general.getOrRestoreManager
import de.yochyo.yummybooru.utils.general.setCurrentManager
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class PictureActivity : AppCompatActivity() {
    companion object {
        private const val MANAGER = "MANAGER"
        fun startActivity(context: Context, manager: ManagerWrapper) {
            context.setCurrentManager(manager)
            context.startActivity(Intent(context, PictureActivity::class.java))
        }
    }

    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var pictureAdapter: PictureAdapter
    private lateinit var tagInfoAdapter: TagInfoAdapter

    private val managerListener = Listener.create<OnUpdateEvent<Post>> { GlobalScope.launch(Dispatchers.Main) { this@PictureActivity.pictureAdapter.updatePosts(it.collection) } }
    lateinit var m: ManagerWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)

        initData(savedInstanceState)
    }

    private fun initData(savedInstanceState: Bundle?) {
        GlobalScope.launch(Dispatchers.Main) {

            withContext(Dispatchers.IO) {
                db.join() //if app is killed in background

                val oldTags = savedInstanceState?.getString("name")
                val oldPos = savedInstanceState?.getInt("position")
                val oldId = savedInstanceState?.getInt("id")
                if (oldTags != null && oldPos != null && oldId != null) m = getOrRestoreManager(oldTags, oldId, oldPos)
                else m = getCurrentManager()!!
            }
            setSupportActionBar(toolbar_picture)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            nav_view_picture.bringToFront()

            tagRecyclerView = nav_view_picture.findViewById(R.id.recycle_view_info)
            tagInfoAdapter = TagInfoAdapter(this@PictureActivity).apply { tagRecyclerView.adapter = this }
            tagRecyclerView.layoutManager = LinearLayoutManager(this@PictureActivity)
            with(view_pager) {
                adapter = PictureAdapter(this@PictureActivity, m).apply { this@PictureActivity.pictureAdapter = this }
                m.posts.registerOnUpdateListener(managerListener)
                currentItem = m.position
                this@PictureActivity.pictureAdapter.updatePosts(m.posts)
                startMedia(m.position)
                m.currentPost?.updateCurrentTags(m.position)
                addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                    override fun onPageScrolled(position: Int, offset: Float, p2: Int) {
                        if (offset == 0.0F && m.position != position) {
                            pauseMedia(m.position)
                            startMedia(position)
                            m.position = position
                            m.currentPost?.updateCurrentTags(position)
                        }
                    }

                    override fun onPageScrollStateChanged(p0: Int) {}
                    override fun onPageSelected(position: Int) {}
                })
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("name", m.toString())
        outState.putInt("position", m.position)
        outState.putInt("id", m.posts.get(if(m.position == -1) 0 else m.position).id)
    }

    private fun Post.updateCurrentTags(wasCurrentPosition: Int) {
        supportActionBar?.title = id.toString()

        tagInfoAdapter.updateInfoTags(emptyList())
        if (wasCurrentPosition == m.position) {
            tagInfoAdapter.updateInfoTags(tags.sortedBy {
                when(it.type){
                    Tag.ARTIST -> 0
                    Tag.COPYPRIGHT -> 1
                    Tag.CHARACTER -> 2
                    Tag.GENERAL -> 3
                    Tag.META -> 4
                    else -> 5
                }
            })
            tagRecyclerView.scrollToPosition(0)
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
            R.id.save -> m.currentPost?.apply { downloadImage(this@PictureActivity, this) }
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

    override fun onPause() {
        pauseMedia(m.position)
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (this::m.isInitialized)
            startMedia(m.position)
    }

    override fun onDestroy() {
        m.posts.removeOnUpdateListener(managerListener)
        super.onDestroy()
    }

    private fun startMedia(position: Int) = getMediaView(position)?.resume()
    private fun pauseMedia(position: Int) = getMediaView(position)?.pause()

    private fun getMediaView(position: Int): MediaView? {
        val child = view_pager.findViewWithTag<View>(position)
        return if (child is LinearLayout) child.getChildAt(0) as MediaView
        else null
    }

}

