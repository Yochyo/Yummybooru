package de.yochyo.yummybooru.layout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.ImageView
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.api.downloads.downloadImage
import de.yochyo.yummybooru.events.events.LoadManagerPageEvent
import de.yochyo.yummybooru.layout.alertdialogs.DownloadPostsAlertdialog
import de.yochyo.yummybooru.utils.preview
import de.yochyo.yummybooru.utils.toTagString
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class PreviewActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context, tags: String) = context.startActivity(Intent(context, PreviewActivity::class.java).apply { putExtra("tags", tags) })
    }

    private var isLoadingView = false
    private var isScrolling = false

    private lateinit var layoutManager: GridLayoutManager
    private lateinit var previewAdapter: PreviewAdapter
    private lateinit var managerListener: Listener<LoadManagerPageEvent>
    private lateinit var m: Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        setSupportActionBar(toolbar_preview)
        m = Manager.getOrInit(intent.getStringExtra("tags"))
        supportActionBar?.title = m.tags.toTagString()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler_view.layoutManager = GridLayoutManager(this, 3).apply { layoutManager = this }
        recycler_view.adapter = PreviewAdapter().apply { previewAdapter = this }

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            reloadView()
        }

        managerListener = LoadManagerPageEvent.registerListener {
            if (it.manager == m)
                previewAdapter.updatePosts()
        }
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
                    if (layoutManager.findLastVisibleItemPosition() + 1 >= m.posts.size) loadPage(m.currentPage + 1)
                return super.onScrollStateChanged(recyclerView, newState)
            }
        })
    }

    private fun reloadView() {
        GlobalScope.launch {
            m.reset()
            loadPage(1)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.download_all -> DownloadPostsAlertdialog(this, m)
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
        LoadManagerPageEvent.removeListener(managerListener)
        super.onDestroy()
    }

    protected inner class PreviewAdapter : RecyclerView.Adapter<PreviewViewHolder>() {
        private var oldSize = 0

        fun updatePosts() {
            if (m.posts.size > oldSize) notifyItemRangeInserted(oldSize, m.posts.size - oldSize)
            else notifyDataSetChanged()
            oldSize = m.posts.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder = PreviewViewHolder((layoutInflater.inflate(R.layout.preview_image_view, parent, false) as ImageView)).apply {
            imageView.setOnClickListener {
                m.position = layoutPosition
                PictureActivity.startActivity(this@PreviewActivity, m.tags.toTagString())
            }
        }

        override fun getItemCount(): Int = m.posts.size
        override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) = holder.imageView.setImageBitmap(null)

        override fun onViewAttachedToWindow(holder: PreviewViewHolder) {
            val pos = holder.adapterPosition
            val p = m.posts[holder.adapterPosition]
            downloadImage(p.filePreviewURL, preview(p.id), {
                if (pos == holder.adapterPosition)
                    holder.imageView.setImageBitmap(it)
            }, isScrolling)
        }
    }

    protected inner class PreviewViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)


}
