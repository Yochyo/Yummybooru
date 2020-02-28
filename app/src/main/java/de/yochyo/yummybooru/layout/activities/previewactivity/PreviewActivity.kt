package de.yochyo.yummybooru.layout.activities.previewactivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.DownloadPostsAlertdialog
import de.yochyo.yummybooru.utils.LoadManagerPageEvent
import de.yochyo.yummybooru.utils.Manager
import de.yochyo.yummybooru.utils.general.drawable
import de.yochyo.yummybooru.utils.general.toTagArray
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class PreviewActivity : AppCompatActivity() {
    private val OFFSET_BEFORE_LOAD_NEXT_PAGE get() = 1 + db.limit / 2

    companion object {
        fun startActivity(context: Context, tags: String) {
            Manager.current = Manager(tags.toTagArray())
            context.startActivity(Intent(context, PreviewActivity::class.java))
        }
    }

    private lateinit var actionBarListener: ActionBarListener
    private var isLoadingView = false
    var isScrolling = false

    private lateinit var layoutManager: GridLayoutManager
    private lateinit var previewAdapter: PreviewAdapter

    private lateinit var m: Manager

    private val managerListener = Listener.create<LoadManagerPageEvent> {
        if (it.newPage.isNotEmpty()) previewAdapter.updatePosts(it.newPage)
        else Toast.makeText(this@PreviewActivity, "End", Toast.LENGTH_SHORT).show()
    }

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
        recycler_view.adapter = PreviewAdapter(this, m).apply { previewAdapter = this }
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


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.download_all -> DownloadPostsAlertdialog(this, m)
            R.id.select_all -> previewAdapter.selectAll()
            R.id.favorite -> {
                val tag = db.getTag(m.tagString)
                if (tag == null) GlobalScope.launch { db.addTag(Api.getTag(this@PreviewActivity, m.tagString).copy(isFavorite = true)) }
                else GlobalScope.launch { db.changeTag(tag.copy(isFavorite = !tag.isFavorite)) }
            }
            R.id.add_tag -> {
                val tag = db.getTag(m.tagString)
                if (tag == null) GlobalScope.launch { db.addTag(Api.getTag(this@PreviewActivity, m.tagString)) }
                else GlobalScope.launch { db.deleteTag(tag.name) }
            }
            R.id.subscribe -> {
                GlobalScope.launch {
                    val sub = db.getSubscription(m.tagString)
                    if (sub != null) db.deleteSubscription(sub.name)
                    else {
                        val tag = db.getTag(m.tagString)
                                ?: Api.getTag(this@PreviewActivity, m.tagString)
                        db.addSubscription(Subscription.fromTag(this@PreviewActivity, tag))
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.preview_menu, menu)
        val tag = db.getTag(m.tagString)
        val sub = db.getSubscription(m.tagString)
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

}
