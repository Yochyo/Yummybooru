package de.yochyo.yummybooru.layout.activities.previewactivity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import de.yochyo.booruapi.api.Post
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.manager.ManagerDistributor
import de.yochyo.yummybooru.api.manager.ManagerWrapper
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.databinding.ActivityPreviewBinding
import de.yochyo.yummybooru.layout.alertdialogs.DownloadPostsDialog
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.layout.selectableRecyclerView.StartSelectingEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.StopSelectingEvent
import de.yochyo.yummybooru.utils.TagUtil
import de.yochyo.yummybooru.utils.distributor.Pointer
import de.yochyo.yummybooru.utils.general.Configuration
import de.yochyo.yummybooru.utils.general.restoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

open class PreviewActivity : AppCompatActivity() {
    lateinit var binding: ActivityPreviewBinding
    private val OFFSET_BEFORE_LOAD_NEXT_PAGE get() = 1 + preferences.limit / 2

    companion object {
        private const val TAGS = "tags"
        private const val POSITION = "pos"
        private const val LAST_ID = "id"

        fun startActivity(context: Context, tags: String) {
            context.startActivity(Intent(context, PreviewActivity::class.java).apply {
                putExtra(TAGS, tags)
            })
        }
    }

    lateinit var viewModel: PreviewActivityViewModel

    private val disableSwipeRefreshOnSelectionListener = Listener<StartSelectingEvent> { binding.content.swipeRefreshLayout.isEnabled = false }
    private val reEnableSwipeRefreshOnSelectionListener =
        Listener<StopSelectingEvent> {
            binding.content.swipeRefreshLayout.isEnabled = true;binding.content.swipeRefreshLayout.isEnabled = false; binding.content.swipeRefreshLayout.isEnabled = true
        }
    private var isLoadingView = false
    var isScrolling = false

    private lateinit var layoutManager: StaggeredGridLayoutManager
    private lateinit var previewAdapter: PreviewAdapter


    lateinit var managerPointer: Pointer<ManagerWrapper>
    val m: ManagerWrapper get() = managerPointer.value


    private val managerListener = Listener<OnAddElementsEvent<Post>> { GlobalScope.launch(Dispatchers.Main) { previewAdapter.updatePosts(it.elements) } }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(PreviewActivityViewModel::class.java)
        viewModel.init(this)
        Configuration.setWindowSecurityFrag(this, window)
        setContentView(binding.root)
        initData(savedInstanceState)
    }

    private fun initData(savedInstanceState: Bundle?) {
        GlobalScope.launch {
            restoreManager(savedInstanceState)
            withContext(Dispatchers.Main) {
                initToolbar()
                m.posts.registerOnAddElementsListener(managerListener)

                binding.content.recyclerView.layoutManager = object : StaggeredGridLayoutManager(preferences.previewColumns, RecyclerView.VERTICAL) {
                    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State?) {
                        try {
                            super.onLayoutChildren(recycler, state)
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    }
                }.apply { layoutManager = this }

                binding.content.recyclerView.adapter = PreviewAdapter(this@PreviewActivity, binding.content.recyclerView, m).apply { previewAdapter = this }
                previewAdapter.isDragSelectingEnabled(binding.content.recyclerView, true)
                previewAdapter.onStartSelection.registerListener(disableSwipeRefreshOnSelectionListener)
                previewAdapter.onStopSelection.registerListener(reEnableSwipeRefreshOnSelectionListener)
                previewAdapter.dragListener.disableAutoScroll()
                initSwipeRefreshLayout()
                initScrollView()
                binding.content.recyclerView.scrollToPosition(m.position)

                loadNextPage()
                loadNextPage()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (this::managerPointer.isInitialized) {
            outState.putString(TAGS, m.toString())
            val pos = m.position
            val id = m.currentPost?.id
            if (m.position > 0) {
                outState.putInt(POSITION, pos)
                if (id != null) outState.putInt(LAST_ID, id)
            }
        }
    }

    fun loadNextPage() {
        isLoadingView = true
        GlobalScope.launch {
            m.downloadNextPage()
            isLoadingView = false
        }
    }

    private fun initScrollView() {
        binding.content.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                when (newState) {
                    RecyclerView.SCROLL_STATE_IDLE -> {
                        isScrolling = false
                        m.position = layoutManager.findFirstCompletelyVisibleItemPositions(null).maxOrNull()!!
                    }
                    RecyclerView.SCROLL_STATE_DRAGGING -> isScrolling = true
                }
                if (!isLoadingView)
                    if (layoutManager.findFirstVisibleItemPositions(null).maxOrNull()!! + OFFSET_BEFORE_LOAD_NEXT_PAGE + preferences.limit >= m.posts.size) loadNextPage()
                return super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1) && m.reachedLastPage)
                    Toast.makeText(this@PreviewActivity, getString(R.string.manager_end), Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun initSwipeRefreshLayout() {
        binding.content.swipeRefreshLayout.setOnRefreshListener {
            binding.content.swipeRefreshLayout.isRefreshing = false
            if (previewAdapter.actionmode == null) {
                GlobalScope.launch(Dispatchers.Main) {
                    m.clear()
                    previewAdapter.updatePosts(m.posts)
                    loadNextPage()
                }
            }
        }
    }

    fun initToolbar() {
        setSupportActionBar(binding.toolbarPreview)
        supportActionBar?.title = m.toString()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.download_all -> DownloadPostsDialog(this, m)
            R.id.select_all -> previewAdapter.selectAll()

            R.id.favorite -> TagUtil.favoriteOrCreateTagIfNotExist(binding.previewActivityContainer, this, m.toString())
            R.id.add_tag -> TagUtil.addTagOrDeleteIfExists(binding.previewActivityContainer, this, m.toString())
            R.id.follow -> TagUtil.CreateFollowedTagOrChangeFollowing(binding.previewActivityContainer, this, m.toString())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.preview_menu, menu)
        viewModel.tags.observe(this, {
            val tag = it.find { it.name == m.toString() }
            Menus.initPreviewMenu(this, menu, tag)
        })
        return true
    }

    override fun onResume() {
        super.onResume()
        if (this::layoutManager.isInitialized)
            layoutManager.scrollToPosition(m.position)
    }

    override fun onDestroy() {
        if (this::managerPointer.isInitialized) {
            m.posts.removeOnAddElementsListener(managerListener)
            ManagerDistributor.releasePointer(m.toString(), managerPointer)
        }
        if (this::previewAdapter.isInitialized) {
            previewAdapter.onStartSelection.removeListener(disableSwipeRefreshOnSelectionListener)
            previewAdapter.onStopSelection.removeListener(reEnableSwipeRefreshOnSelectionListener)
        }
        super.onDestroy()
    }


    private suspend fun restoreManager(savedInstanceState: Bundle?) {
        withContext(Dispatchers.IO) {
            val oldTags = savedInstanceState?.getString(TAGS) ?: intent.extras?.getString(TAGS) ?: "*"
            val oldPos = savedInstanceState?.getInt(POSITION) ?: 0
            val oldId = savedInstanceState?.getInt(LAST_ID) ?: 0

            managerPointer = ManagerDistributor.getPointer(this@PreviewActivity, oldTags)
            if (oldPos != 0 && oldId != 0) m.restoreManager(this@PreviewActivity, oldId, oldPos)
        }
    }
}
