package de.yochyo.yBooru.manager

import android.content.Context
import android.content.Intent
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import de.yochyo.yBooru.R
import de.yochyo.yBooru.api.Api
import de.yochyo.yBooru.api.Post
import de.yochyo.yBooru.layout.PictureActivity
import de.yochyo.yBooru.utils.addChild
import de.yochyo.yBooru.utils.cache
import kotlinx.coroutines.*

class PreviewManager(private val context: Context, val view: RecyclerView, private vararg val tags: String) {
    var root = SupervisorJob()
    val m = Manager.getOrInit(tags.joinToString(" "))
    private val layoutManager = GridLayoutManager(context, 3)
    private val adapter = Adapter()

    private var isLoadingView = false

    init {
        view.layoutManager = layoutManager
        view.adapter = adapter
        scrollView()
        loadPage(1)
    }

    fun loadPage(page: Int) {
        isLoadingView = true
        addChild(root) {
            async {
                val posts = Api.getPosts(page + 1, *tags)
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
                isLoadingView = false
                for (post in posts) {
                    val index = i++
                    addChild(root, isAsync = true) {
                        //TODO deaktivieren um daten zu sparen
                        withContext(Dispatchers.Default) { Api.downloadImage(post.filePreviewURL, "${post.id}Preview") }
                        if (isActive) {
                            m.dataSet[index] = post
                            launch(Dispatchers.Main) { adapter.notifyItemChanged(index) }
                            finishedCount++
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

    private fun scrollView() {
        view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!isLoadingView) {
                    if (layoutManager.findLastVisibleItemPosition() + 1 >= m.dataSet.size) {
                        loadPage(++m.currentPage)
                        println(m.currentPage)
                    }
                }
            }
        })
    }

    private suspend fun getOrDownloadPage(page: Int, vararg tags: String): List<Post> {
        var p = m.pages[page]
        if (p == null) p = Api.getPosts(page, *tags)
        if (m.dataSet.isNotEmpty()) {
            val lastFromLastPage = m.dataSet.last()
            if (lastFromLastPage != null) {
                val samePost = p.find { it.id == lastFromLastPage.id }
                if (samePost != null)
                    p.takeWhile { println(it);it.id != samePost.id }
            }
        }
        return p
    }

    private inner class Adapter : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.recycle_view_grid, parent, false) as ImageView)).apply {
            imageView.setOnClickListener(this)
        }

        override fun getItemCount(): Int = m.dataSet.size
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val post = m.dataSet[position]
            if (post != null) {
                val bitmap = cache.getCachedBitmap("${post.id}Preview")
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
            val intent = Intent(context, PictureActivity::class.java)
            intent.putExtra("tags", tags.joinToString(" "))
            context.startActivity(intent)
        }

    }
}
