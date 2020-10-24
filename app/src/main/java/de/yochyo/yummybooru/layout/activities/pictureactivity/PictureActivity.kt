package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import de.yochyo.booruapi.objects.Post
import de.yochyo.booruapi.objects.Tag
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.manager.ManagerDistributor
import de.yochyo.yummybooru.api.manager.ManagerWrapper
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.views.mediaview.MediaView
import de.yochyo.yummybooru.utils.distributor.Pointer
import de.yochyo.yummybooru.utils.general.downloadAndSaveImage
import de.yochyo.yummybooru.utils.general.restoreManager
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.picture_activity_drawer.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.collections.sortedWith as sortedWith1

class PictureActivity : AppCompatActivity() {

    companion object {
        private const val TAGS = "tags"
        private const val POSITION = "pos"
        private const val LAST_ID = "id"

        fun startActivity(context: Context, manager: ManagerWrapper) {
            context.startActivity(Intent(context, PictureActivity::class.java).apply {
                putExtra(TAGS, manager.toString())
            })
        }
    }

    lateinit var managerPointer: Pointer<ManagerWrapper>
    val m: ManagerWrapper get() = managerPointer.value

    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var tagInfoAdapter: TagInfoAdapter

    private lateinit var pictureAdapter: PictureAdapter

    private val managerListener = Listener<OnUpdateEvent<Post>>
    { GlobalScope.launch(Dispatchers.Main) { this@PictureActivity.pictureAdapter.updatePosts() } }


    //TODO wird onDetach automatisch aufgerufen wenn die activity pausiert wird?

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        setSupportActionBar(toolbar_picture2)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        nav_view_picture.bringToFront()

        tagRecyclerView = nav_view_picture.findViewById(R.id.recycle_view_info)
        tagInfoAdapter = TagInfoAdapter(this).apply { tagRecyclerView.adapter = this }
        tagRecyclerView.layoutManager = LinearLayoutManager(this)


        GlobalScope.launch(Dispatchers.Main) {
            restoreManager(savedInstanceState)
            with(view_pager2) {
                this.isUserInputEnabled = false
                pictureAdapter = PictureAdapter(this@PictureActivity).apply { this@with.adapter = this }
                this.offscreenPageLimit = db.preloadedImages
                m.posts.registerOnUpdateListener(managerListener)
                pictureAdapter.updatePosts()
                m.currentPost?.updateCurrentTags()
                setCurrentItem(m.position, false)
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                        if (positionOffset == 0.0F && m.position != position) {
                            m.position = position
                            m.currentPost?.updateCurrentTags()
                        }
                    }

                    private var lastSelected = m.position

                    override fun onPageSelected(position: Int) {
                        if (lastSelected != -1) {
                            getMediaView(lastSelected)?.pause()
                        }
                        getMediaView(position)?.resume()
                        lastSelected = position
                        if (position + 3 >= m.posts.size - 1) GlobalScope.launch { m.downloadNextPage() }
                    }

                })
            }

        }
    }

    private fun getMediaView(position: Int): MediaView? {
        val child = (view_pager2.findViewWithTag<View>(position) as ViewGroup?)?.getChildAt(0)
        return if (child is MediaView) child
        else null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::managerPointer.isInitialized) {
            outState.putString(TAGS, m.toString())
            if (m.position > 0) {
                outState.putInt(POSITION, m.position)
                if (m.posts.isNotEmpty())
                    outState.putInt(LAST_ID, m.posts[m.position].id)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.picture_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.show_info -> drawer_picture2.openDrawer(GravityCompat.END)
            R.id.save -> m.currentPost?.apply { downloadAndSaveImage(this@PictureActivity, this) }
            R.id.share -> {
                val post = m.currentPost
                if (post != null) {
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, if (post.extention == "zip" && db.downloadWebm) post.fileSampleURL else post.fileURL)
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(intent, null))
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        if (this::managerPointer.isInitialized) {
            m.posts.removeOnUpdateListener(managerListener)
            ManagerDistributor.releasePointer(m.toString(), managerPointer)
        }
        super.onDestroy()
    }

    override fun onPause() {
        super.onPause()
        if (this::managerPointer.isInitialized)
            getMediaView(m.position)?.pause()
    }

    override fun onResume() {
        super.onResume()
        if (this::managerPointer.isInitialized)
            getMediaView(m.position)?.resume()
    }

    private fun Post.updateCurrentTags() {
        supportActionBar?.title = id.toString()

        GlobalScope.launch {
            val sorted = tags.sortedWith1 { o1, o2 ->
                fun sortedType(type: Int): Int {
                    return when (type) {
                        Tag.ARTIST -> 0
                        Tag.COPYPRIGHT -> 1
                        Tag.CHARACTER -> 2
                        Tag.GENERAL -> 3
                        Tag.META -> 4
                        else -> 5
                    }
                }

                val sortedType1 = sortedType(o1.type)
                val sortedType2 = sortedType(o2.type)
                if (sortedType1 == sortedType2) o1.name.compareTo(o2.name)
                else sortedType1 - sortedType2
            }
            withContext(Dispatchers.Main) {
                tagInfoAdapter.updateInfoTags(sorted)
                tagRecyclerView.scrollToPosition(0)
            }
        }
    }

    private suspend fun restoreManager(savedInstanceState: Bundle?) {
        withContext(Dispatchers.IO) {
            val oldTags = savedInstanceState?.getString(TAGS) ?: intent.extras?.getString(TAGS) ?: "*"
            val oldPos = savedInstanceState?.getInt(POSITION) ?: 0
            val oldId = savedInstanceState?.getInt(LAST_ID) ?: 0

            managerPointer = ManagerDistributor.getPointer(this@PictureActivity, oldTags)
            if (oldPos != 0 && oldId != 0) m.restoreManager(this@PictureActivity, oldId, oldPos)
        }
    }
}