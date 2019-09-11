package de.yochyo.yummybooru.layout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ActionMode
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.downloads.LoadManagerPageEvent
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.api.downloads.downloadImage
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.DownloadService
import de.yochyo.yummybooru.layout.alertdialogs.DownloadPostsAlertdialog
import de.yochyo.yummybooru.layout.views.SelectableRecyclerViewAdapter
import de.yochyo.yummybooru.layout.views.SelectableViewHolder
import de.yochyo.yummybooru.utils.preview
import de.yochyo.yummybooru.utils.toTagArray
import de.yochyo.yummybooru.utils.toTagString
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

open class PreviewActivity : AppCompatActivity() {
    companion object {
        private val OFFSET_BEFORE_LOAD_NEXT_PAGE get() = 1 + db.limit / 2
        fun startActivity(context: Context, tags: String) {
            Manager.current = Manager(tags.toTagArray())
            context.startActivity(Intent(context, PreviewActivity::class.java))
        }
    }

    private var isLoadingView = false
    private var isScrolling = false

    private lateinit var layoutManager: GridLayoutManager
    private lateinit var previewAdapter: PreviewAdapter
    private val managerListener: Listener<LoadManagerPageEvent> = object : Listener<LoadManagerPageEvent>() {
        override fun onEvent(e: LoadManagerPageEvent) {
            if (e.newPage.isNotEmpty()) previewAdapter.updatePosts(e.newPage)
            else Toast.makeText(this@PreviewActivity, "End", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var m: Manager

    protected var actionmode: ActionMode? = null
    protected val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(p0: ActionMode, p1: Menu): Boolean {
            p0.menuInflater.inflate(R.menu.preview_activity_selection_menu, p1)
            p0.title = "${previewAdapter.selected.size}/${m.posts.size}"
            return true
        }

        override fun onActionItemClicked(p0: ActionMode, p1: MenuItem): Boolean {
            when (p1.itemId) {
                R.id.select_all -> if (previewAdapter.selected.size == m.posts.size) previewAdapter.unselectAll() else previewAdapter.selectAll()
                R.id.download_selected -> {
                    val posts = LinkedList<Post>()
                    for (i in 0 until previewAdapter.selected.size)
                        posts += m.posts[i]
                    DownloadService.startService(this@PreviewActivity, m.tags.toTagString(), posts, Server.currentServer)
                    previewAdapter.unselectAll()
                }
                else -> return false
            }
            return true
        }

        override fun onDestroyActionMode(p0: ActionMode) {
            previewAdapter.unselectAll()
        }

        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?) = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        val manager = Manager.current
        if (manager != null) m = manager else finish()
        setSupportActionBar(toolbar_preview)
        initToolbar()

        m.loadManagerPageEvent.registerListener(managerListener)
        recycler_view.layoutManager = GridLayoutManager(this, 3).apply { layoutManager = this }
        recycler_view.adapter = PreviewAdapter().apply { previewAdapter = this }
        initSwipeRefreshLayout()
        initScrollView()

        loadPage(1)
    }

    fun loadPage(page: Int) {
        isLoadingView = true
        GlobalScope.launch {
            m.downloadPage(page)
            launch(Dispatchers.Main) {
                m.loadPage(this@PreviewActivity, page)
                isLoadingView = false
            }
            launch { m.downloadPage(page + 1) }
        }
    }

    private fun initScrollView() {
        recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> isScrolling = false
                    RecyclerView.SCROLL_STATE_DRAGGING -> isScrolling = true
                }
                if (!isLoadingView)
                    if (layoutManager.findLastVisibleItemPosition() + OFFSET_BEFORE_LOAD_NEXT_PAGE >= m.posts.size) loadPage(m.currentPage + 1)
                return super.onScrollStateChanged(recyclerView, newState)
            }
        })
    }

    fun initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            previewAdapter.unselectAll()
            GlobalScope.launch {
                m.reset()
                loadPage(1)
            }
        }
    }

    fun initToolbar() {
        supportActionBar?.title = m.tags.toTagString()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected inner class PreviewAdapter : SelectableRecyclerViewAdapter<PreviewViewHolder>() {
        fun updatePosts(newPage: Collection<Post>) {
            if (newPage.isNotEmpty())
                if (m.posts.size > newPage.size) notifyItemRangeInserted(m.posts.size - newPage.size, newPage.size)
                else notifyDataSetChanged()
        }

        override val onClickLayout = { holder: PreviewViewHolder ->
            m.position = holder.layoutPosition
            PictureActivity.startActivity(this@PreviewActivity, m)
        }

        override fun createViewHolder(parent: ViewGroup) = PreviewViewHolder((layoutInflater.inflate(R.layout.preview_image_view, parent, false) as FrameLayout))
        override fun onViewAttachedToWindow(holder: PreviewViewHolder) {
            val pos = holder.adapterPosition
            val p = m.posts[holder.adapterPosition]
            downloadImage(p.filePreviewURL, preview(p.id), {
                if (pos == holder.adapterPosition)
                    holder.layout.findViewById<ImageView>(R.id.preview_picture).setImageBitmap(it)
            }, isScrolling)
        }

        override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            holder.layout.findViewById<ImageView>(R.id.preview_picture).setImageBitmap(null)
        }


        override fun onStartSelecting() {
            if (actionmode == null) {
                actionmode = startSupportActionMode(actionModeCallback)
                m.loadManagerPageEvent.registerListener(loadManagerPageUpdateActionModeListener)
            }
        }

        override fun onStopSelecting() {
            actionmode?.finish()
            actionmode = null
            m.loadManagerPageEvent.removeListener(loadManagerPageUpdateActionModeListener)
        }

        override fun onUpdate() {
            actionmode?.title = "${selected.size}/${m.posts.size}"
        }

        override fun getItemCount(): Int = m.posts.size
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.download_all -> DownloadPostsAlertdialog(this, m)
            R.id.select_all -> previewAdapter.selectAll()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.preview_menu, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        layoutManager.scrollToPosition(m.position)
    }

    override fun onDestroy() {
        m.loadManagerPageEvent.removeListener(managerListener)
        super.onDestroy()
    }

    private val loadManagerPageUpdateActionModeListener = object : Listener<LoadManagerPageEvent>() {
        override fun onEvent(e: LoadManagerPageEvent) {
            actionmode?.title = "${previewAdapter.selected.size}/${m.posts.size}"
        }
    }

    inner class PreviewViewHolder(layout: FrameLayout) : SelectableViewHolder(layout)
}
