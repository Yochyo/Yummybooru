package de.yochyo.ybooru.layout

import android.arch.lifecycle.Observer
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
import android.widget.Toast
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.api.downloadImage
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.preview
import de.yochyo.ybooru.utils.toTagString
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class PreviewActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context, tags: String) = context.startActivity(Intent(context, PreviewActivity::class.java).apply { putExtra("tags", tags) })
    }

    private val observer = Observer<ArrayList<Post>> { if(it != null) previewAdapter.updatePosts(it) }

    protected var isLoadingView = false
    protected var isScrolling = false

    protected lateinit var layoutManager: GridLayoutManager
    protected lateinit var previewAdapter: PreviewAdapter
    protected lateinit var m: Manager

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

        initScrollView()
        loadPage(1)
        m.posts.observe(this, observer)
    }

    fun loadPage(page: Int) {
        isLoadingView = true
        GlobalScope.launch {
            m.downloadPage(page)
            launch(Dispatchers.Main) {
                m.loadPage(page)
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
                    if (layoutManager.findLastVisibleItemPosition() + 1 >= previewAdapter.posts.size) loadPage(m.currentPage + 1)
                return super.onScrollStateChanged(recyclerView, newState)
            }
        })
    }

    fun reloadView() {
        m.reset()
        loadPage(1)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.download_all -> Toast.makeText(this, getString(R.string.not_inplemented), Toast.LENGTH_SHORT).show()
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
        m.posts.removeObserver(observer)
        super.onDestroy()
    }

    protected inner class PreviewAdapter : RecyclerView.Adapter<PreviewViewHolder>() {
        private var oldSize = 0
        var posts = ArrayList<Post>()

        fun updatePosts(array: ArrayList<Post>) {
            posts = array
            notifyItemRangeInserted(oldSize, array.size - oldSize)
            oldSize = array.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder = PreviewViewHolder((layoutInflater.inflate(R.layout.preview_image_view, parent, false) as ImageView)).apply {
            imageView.setOnClickListener {
                m.position = layoutPosition
                PictureActivity.startActivity(this@PreviewActivity, m.tags.toTagString())
            }
        }

        override fun getItemCount(): Int = posts.size
        override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
            holder.imageView.setImageBitmap(null)
        }


        override fun onViewAttachedToWindow(holder: PreviewViewHolder) {
            val pos = holder.adapterPosition
            //hier könnte es diesen einen absturz geben
            if (pos != -1) {
                val p = m.posts[holder.adapterPosition]
                downloadImage(p.filePreviewURL, preview(p.id), {
                    if (pos == holder.adapterPosition)
                        holder.imageView.setImageBitmap(it)
                }, isScrolling)
            }
        }
    }

    protected inner class PreviewViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)


}
