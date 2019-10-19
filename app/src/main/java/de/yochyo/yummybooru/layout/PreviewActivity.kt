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
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.downloads.LoadManagerPageEvent
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.api.downloads.downloadImage
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.DownloadService
import de.yochyo.yummybooru.events.events.AddTagEvent
import de.yochyo.yummybooru.events.events.ChangeTagEvent
import de.yochyo.yummybooru.events.events.DeleteTagEvent
import de.yochyo.yummybooru.layout.alertdialogs.DownloadPostsAlertdialog
import de.yochyo.yummybooru.layout.views.SelectableRecyclerViewAdapter
import de.yochyo.yummybooru.layout.views.SelectableViewHolder
import de.yochyo.yummybooru.utils.drawable
import de.yochyo.yummybooru.utils.preview
import de.yochyo.yummybooru.utils.toTagArray
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
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
    private val managerListener = object : Listener<LoadManagerPageEvent>() {
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
        m = Manager.current
        setSupportActionBar(toolbar_preview)
        initToolbar()

        m.loadManagerPageEvent.registerListener(managerListener)
        recycler_view.layoutManager = GridLayoutManager(this, 3).apply { layoutManager = this }
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
            launch { m.downloadPage(this@PreviewActivity, m.currentPage+1) }
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
            if (actionmode == null) {
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
                    it.loadInto(holder.layout.findViewById<ImageView>(R.id.preview_picture))
            }, isScrolling)
        }

        override fun onCreateViewHolder(parent: ViewGroup, position: Int): PreviewViewHolder {
            val holder = super.onCreateViewHolder(parent, position)
            return holder
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
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.preview_menu, menu)
        val tag = db.tags.find { it.name == m.tagString }
        if (tag == null)
            menu.findItem(R.id.add_tag).icon = drawable(R.drawable.add)
        else if (tag.isFavorite) menu.findItem(R.id.favorite).icon = drawable(R.drawable.favorite)
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

    private val loadManagerPageUpdateActionModeListener = object : Listener<LoadManagerPageEvent>() {
        override fun onEvent(e: LoadManagerPageEvent) {
            actionmode?.title = "${previewAdapter.selected.size}/${m.posts.size}"
        }
    }

    inner class PreviewViewHolder(layout: FrameLayout) : SelectableViewHolder(layout)
}

private class ActionBarListener(val context: Context, tag: String, menu: Menu){
    private var registered: Boolean = false
    fun registerListeners(){
        if(!registered){
            registered = true
            AddTagEvent.registerListener(addTagListener)
            DeleteTagEvent.registerListener(removeTagListener)
            ChangeTagEvent.registerListener(favoriteTagListener)
        }
    }
    fun unregisterListeners(){
        if(registered){
            registered = false
            AddTagEvent.removeListener(addTagListener)
            DeleteTagEvent.removeListener(removeTagListener)
            ChangeTagEvent.removeListener(favoriteTagListener)
        }
    }

    private val addTagListener = object: Listener<AddTagEvent>(){
        override fun onEvent(e: AddTagEvent) {
            if(e.tag.name == tag) {
                menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.remove)
                if(e.tag.isFavorite) menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.favorite)
            }

        }
    }
    private val removeTagListener = object: Listener<DeleteTagEvent>(){
        override fun onEvent(e: DeleteTagEvent) {
            if(e.tag.name == tag) {
                menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.add)
                menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
            }
        }
    }
    private val favoriteTagListener = object: Listener<ChangeTagEvent>(){
        override fun onEvent(e: ChangeTagEvent) {
            if(e.newTag.name == tag){
                if(e.newTag.isFavorite) menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.favorite)
                else menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
            }
        }
    }
}
