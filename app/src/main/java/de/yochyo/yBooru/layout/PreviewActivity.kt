package de.yochyo.yBooru.layout

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import de.yochyo.yBooru.R
import de.yochyo.yBooru.api.Api
import de.yochyo.yBooru.api.Post
import de.yochyo.yBooru.manager.Manager
import de.yochyo.yBooru.preview
import de.yochyo.yBooru.utils.addChild
import de.yochyo.yBooru.utils.cache
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
import kotlinx.coroutines.*

class PreviewActivity : AppCompatActivity() {
    private var root = SupervisorJob()
    private lateinit var tags: Array<String>

    private lateinit var m: Manager
    private var isLoadingView = false

    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        setSupportActionBar(toolbar)

        tags = intent.getStringExtra("tags").split(" ").toTypedArray()
        m = Manager.getOrInit(tags.joinToString(" "))

        recycler_view.layoutManager = GridLayoutManager(this, 3).apply { layoutManager = this }
        recycler_view.adapter = Adapter().apply { adapter = this }

        initScrollView()
        initSwipeRefreshLayout()
        loadPage(1)
    }

    fun loadPage(page: Int) {
        isLoadingView = true
        addChild(root) {
            async {
                val posts = Api.getPosts(this@PreviewActivity, page + 1, *tags)
                if (m.pages[page + 1] == null)
                    m.pages[page + 1] = posts
            }

            val posts = getOrDownloadPage(page, *tags)

            launch(Dispatchers.Main) {
                var finishedCount = 0
                var i = m.dataSet.size

                for (t in 0 until posts.size)
                    m.dataSet.add(null)
                adapter.notifyItemRangeInserted(i - 1, posts.size)
                for (t in 0 until posts.size)
                    m.dataSet[i + t] = posts[t]
                isLoadingView = false

                addChild(root, isAsync = true) {
                    for (post in posts) {
                        val index = i++
                        Api.downloadImage(this@PreviewActivity, post.filePreviewURL, preview(post.id))
                        if (isActive) {
                            m.dataSet[index] = post
                            finishedCount++
                            withContext(Dispatchers.Main) {
                                adapter.notifyItemChanged(index)
                            }
                        }
                    }
                }
            }
        }
    }


    fun reloadView() {
        root.cancel()
        root = SupervisorJob()
        m.dataSet.clear()
        m.pages.clear()
        adapter.notifyDataSetChanged()
        loadPage(1)
    }

    private fun initScrollView() {
        recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!isLoadingView) {
                    if (layoutManager.findLastVisibleItemPosition() + 1 >= m.dataSet.size) {
                        loadPage(++m.currentPage)
                    }
                }
            }
        })
    }

    private suspend fun getOrDownloadPage(page: Int, vararg tags: String): List<Post> {
        var p = m.pages[page]
        if (p == null) p = Api.getPosts(this, page, *tags)
        if (m.dataSet.isNotEmpty()) {
            val lastFromLastPage = m.dataSet.last()
            if (lastFromLastPage != null) {
                val samePost = p.find { it.id == lastFromLastPage.id }
                if (samePost != null)
                    p.takeWhile { it.id != samePost.id }
            }
        }
        return p
    }

    private fun initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            reloadView()
        }
    }

    private inner class Adapter : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.preview_image_view, parent, false) as ImageView)).apply {
            imageView.setOnClickListener(this)
        }

        override fun getItemCount(): Int = m.dataSet.size
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val post = m.dataSet[position]
            if (post != null) {
                val bitmap = this@PreviewActivity.cache.getCachedBitmap(preview(post.id))
                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap)
                    return
                }
            }
            holder.imageView.setImageDrawable(null)
        }
    }

    private inner class MyViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView), View.OnClickListener {
        override fun onClick(v: View?) {
            m.position = layoutPosition
            val intent = Intent(this@PreviewActivity, PictureActivity::class.java)
            intent.putExtra("tags", tags.joinToString(" "))
            this@PreviewActivity.startActivity(intent)
        }
    }
}
