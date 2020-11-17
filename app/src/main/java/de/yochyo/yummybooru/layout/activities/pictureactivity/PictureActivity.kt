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
import com.github.chrisbanes.photoview.PhotoView
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.api.TagType
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
                pictureAdapter = PictureAdapter(this@PictureActivity).apply { this@with.adapter = this }
                this.offscreenPageLimit = db.preloadedImages
                m.posts.registerOnUpdateListener(managerListener)
                pictureAdapter.updatePosts()
                setCurrentItem(m.position, false)
                onPageSelected(-1, m.position)
                registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}


                    var lastSelected = -1
                    override fun onPageSelected(position: Int) {
                        onPageSelected(lastSelected, position)
                        lastSelected = position
                    }

                })
            }

        }
    }

    fun onPageSelected(old: Int, position: Int) {
        m.position = position
        m.currentPost?.updateCurrentTags()
        if (old != -1) getMediaView(old)?.pause()
        getMediaView(position)?.resume()
        if (position + 2 + db.preloadedImages >= m.posts.size - 1) GlobalScope.launch { m.downloadNextPage() }
        view_pager2.isUserInputEnabled = if (old == -1) false
        else getPhotoView(position)?.scale != 1f
    }

    private fun getMediaView(position: Int): MediaView? {
        val child = getViewAtPosition(position)
        return if (child is MediaView) child
        else null
    }

    private fun getPhotoView(position: Int): PhotoView? {
        val child = getViewAtPosition(position)
        return if (child is PhotoView) child
        else null
    }

    private fun getViewAtPosition(position: Int): View? {
        return (view_pager2.findViewWithTag<View>(position) as ViewGroup?)?.getChildAt(0)
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
                        putExtra(Intent.EXTRA_TEXT, if (post.extension == "zip" && db.downloadWebm) post.fileSampleURL else post.fileURL)
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
            val sorted = getTags().sortedWith1 { o1, o2 ->
                fun sortedType(type: TagType): Int {
                    return when (type) {
                        TagType.ARTIST -> 0
                        TagType.COPYRIGHT -> 1
                        TagType.CHARACTER -> 2
                        TagType.GENERAL -> 3
                        TagType.META -> 4
                        else -> 5
                    }
                }

                val sortedType1 = sortedType(o1.tagType)
                val sortedType2 = sortedType(o2.tagType)
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