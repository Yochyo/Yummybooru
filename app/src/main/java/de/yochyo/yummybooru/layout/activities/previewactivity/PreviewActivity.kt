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
import de.yochyo.booruapi.objects.Post
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.DownloadPostsAlertdialog
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.layout.selectableRecyclerView.StartSelectingEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.StopSelectingEvent
import de.yochyo.yummybooru.utils.ManagerWrapper
import de.yochyo.yummybooru.utils.general.createTagAndOrChangeSubState
import de.yochyo.yummybooru.utils.general.currentManager
import de.yochyo.yummybooru.utils.general.currentServer
import de.yochyo.yummybooru.utils.general.drawable
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

open class PreviewActivity : AppCompatActivity() {
    private val OFFSET_BEFORE_LOAD_NEXT_PAGE get() = 1 + db.limit / 2

    companion object {
        fun startActivity(context: Context, tags: String) {
            context.currentManager = ManagerWrapper.build(context, tags)
            context.startActivity(Intent(context, PreviewActivity::class.java))
        }
    }

    private lateinit var actionBarListener: ActionBarListener

    private val disableSwipeRefreshOnSelectionListener = Listener.create<StartSelectingEvent> { swipeRefreshLayout.isEnabled = false }
    private val reEnableSwipeRefreshOnSelectionListener = Listener.create<StopSelectingEvent> { swipeRefreshLayout.isEnabled = true;swipeRefreshLayout.isEnabled = false }
    private var isLoadingView = false
    var isScrolling = false

    private lateinit var layoutManager: GridLayoutManager
    private lateinit var previewAdapter: PreviewAdapter

    private lateinit var m: ManagerWrapper

    private val managerListener = Listener.create<OnAddElementsEvent<Post>> {
        GlobalScope.launch(Dispatchers.Main) {
            if(it.elements.isEmpty()) Toast.makeText(this@PreviewActivity, "End", Toast.LENGTH_SHORT).show()
            else previewAdapter.updatePosts(it.elements)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        m = currentManager
        initToolbar()

        m.posts.registerOnAddElementsListener(managerListener)
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
        previewAdapter.isDragSelectingEnabled(recycler_view, true)
        previewAdapter.onStartSelection.registerListener(disableSwipeRefreshOnSelectionListener)
        previewAdapter.onStopSelection.registerListener(reEnableSwipeRefreshOnSelectionListener)
        previewAdapter.dragListener.disableAutoScroll()

        initSwipeRefreshLayout()
        initScrollView()

        loadNextPage()
        loadNextPage()
    }

    fun loadNextPage() {
        isLoadingView = true
        GlobalScope.launch {
            m.downloadNextPage()
            isLoadingView = false
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
                    if (layoutManager.findLastVisibleItemPosition() + OFFSET_BEFORE_LOAD_NEXT_PAGE + db.limit >= m.posts.size) loadNextPage()
                return super.onScrollStateChanged(recyclerView, newState)
            }
        })
    }

    fun initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            if (previewAdapter.actionmode == null) {
                GlobalScope.launch {
                    m.clear()
                    loadNextPage()
                }
            }
        }
    }

    fun initToolbar() {
        setSupportActionBar(toolbar_preview)
        supportActionBar?.title = m.toString()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.download_all -> DownloadPostsAlertdialog(this, m)
            R.id.select_all -> previewAdapter.selectAll()
            R.id.favorite -> {
                val tag = db.getTag(m.toString())

                if (tag == null) GlobalScope.launch {
                    val t = currentServer.getTag(this@PreviewActivity, m.toString())
                    if (t != null) db.tags += t.apply { isFavorite = true }
                }
                else tag.isFavorite = !tag.isFavorite
            }
            R.id.add_tag -> {
                val tag = db.getTag(m.toString())
                if (tag == null) GlobalScope.launch {
                    val t = currentServer.getTag(this@PreviewActivity, m.toString())
                    if (t != null) db.tags += t
                }
                else db.tags -= tag
            }
            R.id.subscribe -> {
                GlobalScope.launch {
                    createTagAndOrChangeSubState(this@PreviewActivity, m.toString())
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.preview_menu, menu)
        val tag = db.getTag(m.toString())
        Menus.initPreviewMenu(this, menu, tag)
        actionBarListener = ActionBarListener(this, m.toString(), menu).apply { registerListeners() }
        return true
    }

    override fun onResume() {
        super.onResume()
        layoutManager.scrollToPosition(m.position)
    }

    override fun onDestroy() {
        m.posts.removeOnAddElementsListener(managerListener)
        actionBarListener.unregisterListeners()
        previewAdapter.onStartSelection.removeListener(disableSwipeRefreshOnSelectionListener)
        previewAdapter.onStopSelection.removeListener(reEnableSwipeRefreshOnSelectionListener)
        super.onDestroy()
    }

}
