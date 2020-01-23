package de.yochyo.yummybooru.layout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.DownloadService
import de.yochyo.yummybooru.events.events.*
import de.yochyo.yummybooru.layout.alertdialogs.DownloadPostsAlertdialog
import de.yochyo.yummybooru.layout.views.*
import de.yochyo.yummybooru.utils.LoadManagerPageEvent
import de.yochyo.yummybooru.utils.Manager
import de.yochyo.yummybooru.utils.general.drawable
import de.yochyo.yummybooru.utils.general.preview
import de.yochyo.yummybooru.utils.general.toTagArray
import de.yochyo.yummybooru.utils.network.downloader
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class PreviewActivity : AppCompatActivity() {
    companion object {
        private val OFFSET_BEFORE_LOAD_NEXT_PAGE get() = 1 + db.limit / 2
        fun startActivity(context: Context, tags: String) {
            Manager.current = Manager(tags.toTagArray())
            context.startActivity(Intent(context, PreviewActivity::class.java))
        }
    }

    private lateinit var actionBarListener: ActionBarListener
    private var isLoadingView = false
    private var isScrolling = false

    private lateinit var layoutManager: GridLayoutManager
    private lateinit var previewAdapter: PreviewAdapter
    private val managerListener = Listener.create<LoadManagerPageEvent> {
        if (it.newPage.isNotEmpty()) previewAdapter.updatePosts(it.newPage)
        else Toast.makeText(this@PreviewActivity, "End", Toast.LENGTH_SHORT).show()
    }


    private lateinit var m: Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        m = Manager.current
        setSupportActionBar(toolbar_preview)
        initToolbar()

        m.loadManagerPageEvent.registerListener(managerListener)
        recycler_view.layoutManager = object : GridLayoutManager(this, 3) {
            override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
                try {
                    super.onLayoutChildren(recycler, state)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            }
        }.apply { layoutManager = this }
        recycler_view.adapter = PreviewAdapter().apply { previewAdapter = this }
        initSwipeRefreshLayout()
        initScrollView()

        loadNextPage()
    }

    fun loadNextPage() {
        isLoadingView = true
        GlobalScope.launch {
            m.loadNextPage(this@PreviewActivity)
            isLoadingView = false
            launch { m.downloadPage(this@PreviewActivity, m.currentPage + 1) }
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
                    if (layoutManager.findLastVisibleItemPosition() + OFFSET_BEFORE_LOAD_NEXT_PAGE >= m.posts.size) loadNextPage()
                return super.onScrollStateChanged(recyclerView, newState)
            }
        })
    }

    fun initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            if (previewAdapter.actionmode == null) {
                GlobalScope.launch {
                    m.reset()
                    loadNextPage()
                }
            }
        }
    }

    fun initToolbar() {
        supportActionBar?.title = m.tagString
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected inner class PreviewAdapter : SelectableRecyclerViewAdapter<PreviewViewHolder>(this, R.menu.preview_activity_selection_menu) {
        private val startSelectionListener = Listener.create<StartSelectingEvent> {
            m.loadManagerPageEvent.registerListener(loadManagerPageUpdateActionModeListener)
            previewAdapter.actionmode?.title = "${selected.size}/${m.posts.size}"
        }
        private val stopSelectionListener = Listener.create<StopSelectingEvent> {
            m.loadManagerPageEvent.removeListener(loadManagerPageUpdateActionModeListener)
        }
        private val updateSelectionListener = Listener.create<UpdateSelectionEvent> {
            previewAdapter.actionmode?.title = "${selected.size}/${m.posts.size}"
        }
        private val clickMenuItemListener = Listener.create<ActionModeClickEvent> {
            when (it.menuItem.itemId) {
                R.id.select_all -> if (previewAdapter.selected.size == m.posts.size) previewAdapter.unselectAll() else previewAdapter.selectAll()
                R.id.download_selected -> {
                    val posts = previewAdapter.selected.getSelected(m.posts)
                    DownloadService.startService(this@PreviewActivity, m.tagString, posts, Server.currentServer)
                    previewAdapter.unselectAll()
                }
                R.id.download_and_add_authors_selected -> {
                    val posts = previewAdapter.selected.getSelected(m.posts)
                    GlobalScope.launch {
                        for (post in posts) {
                            for (tag in post.getTags()) {
                                if (tag.type == Tag.ARTIST) db.addTag(this@PreviewActivity, tag)
                            }
                        }
                    }
                    DownloadService.startService(this@PreviewActivity, m.tagString, posts, Server.currentServer)
                    previewAdapter.unselectAll()
                }
            }
        }

        init {
            onStartSelection.registerListener(startSelectionListener)
            onStopSelection.registerListener(stopSelectionListener)
            onUpdateSelection.registerListener(updateSelectionListener)
            onClickMenuItem.registerListener(clickMenuItemListener)
        }

        fun updatePosts(newPage: Collection<Post>) {
            notifyItemRangeInserted(m.posts.size - newPage.size, newPage.size)
        }

        override val onClickLayout = { holder: PreviewViewHolder ->
            m.position = holder.layoutPosition
            PictureActivity.startActivity(this@PreviewActivity, m)
        }

        override fun createViewHolder(parent: ViewGroup) = PreviewViewHolder((layoutInflater.inflate(R.layout.preview_image_view, parent, false) as FrameLayout))

        override fun onViewAttachedToWindow(holder: PreviewViewHolder) {
            val pos = holder.adapterPosition
            val p = m.posts[holder.adapterPosition]
            downloader.downloadAndCache(this@PreviewActivity, p.filePreviewURL, {
                if (pos == holder.adapterPosition)
                    GlobalScope.launch(Dispatchers.Main) { it.loadInto(holder.layout.findViewById<ImageView>(R.id.preview_picture)) }
            }, isScrolling, preview(p.id))
        }

        override fun onCreateViewHolder(parent: ViewGroup, position: Int): PreviewViewHolder {
            val holder = super.onCreateViewHolder(parent, position)
            return holder
        }

        override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            holder.layout.findViewById<ImageView>(R.id.preview_picture).setImageBitmap(null)
        }

        override fun getItemCount(): Int = m.posts.size
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.download_all -> DownloadPostsAlertdialog(this, m)
            R.id.select_all -> previewAdapter.selectAll()
            R.id.favorite -> {
                val tag = db.tags.find { it.name == m.tagString }
                if (tag == null) GlobalScope.launch { db.addTag(this@PreviewActivity, Api.getTag(m.tagString).copy(isFavorite = true)) }
                else GlobalScope.launch { db.changeTag(this@PreviewActivity, tag.copy(isFavorite = !tag.isFavorite)) }
            }
            R.id.add_tag -> {
                val tag = db.tags.find { it.name == m.tagString }
                if (tag == null) GlobalScope.launch { db.addTag(this@PreviewActivity, Api.getTag(m.tagString)) }
                else GlobalScope.launch { db.deleteTag(this@PreviewActivity, tag.name) }
            }
            R.id.subscribe -> {
                GlobalScope.launch {
                    val sub = db.subs.find { it.name == m.tagString }
                    if (sub != null) db.deleteSubscription(this@PreviewActivity, sub.name)
                    else {
                        val tag = db.tags.find { it.name == m.tagString } ?: Api.getTag(m.tagString)
                        db.addSubscription(this@PreviewActivity, Subscription.fromTag(tag))
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.preview_menu, menu)
        val tag = db.tags.find { it.name == m.tagString }
        val sub = db.subs.find { it.name == m.tagString }
        if (tag == null)
            menu.findItem(R.id.add_tag).icon = drawable(R.drawable.add)
        else if (tag.isFavorite) menu.findItem(R.id.favorite).icon = drawable(R.drawable.favorite)
        if (sub != null) menu.findItem(R.id.subscribe).icon = drawable(R.drawable.star)
        actionBarListener = ActionBarListener(this, m.tagString, menu).apply { registerListeners() }
        return true
    }

    override fun onResume() {
        super.onResume()
        layoutManager.scrollToPosition(m.position)
    }

    override fun onDestroy() {
        m.loadManagerPageEvent.removeListener(managerListener)
        actionBarListener.unregisterListeners()
        super.onDestroy()
    }

    private val loadManagerPageUpdateActionModeListener = Listener.create<LoadManagerPageEvent> {
        previewAdapter.actionmode?.title = "${previewAdapter.selected.size}/${m.posts.size}"
    }

    inner class PreviewViewHolder(layout: FrameLayout) : SelectableViewHolder(layout)
}

private class ActionBarListener(val context: Context, tag: String, menu: Menu) {
    private var registered: Boolean = false
    fun registerListeners() {
        if (!registered) {
            registered = true
            AddTagEvent.registerListener(addTagListener)
            DeleteTagEvent.registerListener(removeTagListener)
            ChangeTagEvent.registerListener(favoriteTagListener)
            AddSubEvent.registerListener(addSubListener)
            DeleteSubEvent.registerListener(deleteSubListener)
        }
    }

    fun unregisterListeners() {
        if (registered) {
            registered = false
            AddTagEvent.removeListener(addTagListener)
            DeleteTagEvent.removeListener(removeTagListener)
            ChangeTagEvent.removeListener(favoriteTagListener)
            AddSubEvent.removeListener(addSubListener)
            DeleteSubEvent.removeListener(deleteSubListener)
        }
    }

    private val addTagListener = Listener.create<AddTagEvent> {
        if (it.tag.name == tag) {
            menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.remove)
            if (it.tag.isFavorite) menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.favorite)
        }
    }
    private val removeTagListener = Listener.create<DeleteTagEvent> {
        if (it.tag.name == tag) {
            menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.add)
            menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
        }
    }
    private val favoriteTagListener = Listener.create<ChangeTagEvent> {
        if (it.newTag.name == tag) {
            if (it.newTag.isFavorite) menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.favorite)
            else menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
        }
    }
    private val addSubListener = Listener.create<AddSubEvent> {
        if (it.sub.name == tag) {
            menu.findItem(R.id.subscribe).icon = context.drawable(R.drawable.star)
        }
    }
    private val deleteSubListener = Listener.create<DeleteSubEvent> {
        if (it.sub.name == tag) {
            menu.findItem(R.id.subscribe).icon = context.drawable(R.drawable.star_empty)
        }
    }
}
