package de.yochyo.yBooru

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import de.yochyo.danbooruAPI.Api
import de.yochyo.yBooru.api.Post
import de.yochyo.yBooru.utils.runAsync

class PreviewManager(val context: Context, val view: RecyclerView) {
    var page = 1
    private val pages = HashMap<Int, List<Post>>() //page, posts
    val currentTags = ArrayList<String>(2)

    var isLoadingView = false

    private val dataSet = ArrayList<Post?>(200)
    private val adapter = Adapter()
    private val layoutManager = GridLayoutManager(context, 3)

    init {
        view.layoutManager = layoutManager
        view.adapter = adapter
        view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!isLoadingView) {
                    if (layoutManager.findLastVisibleItemPosition() + 1 >= dataSet.size) {
                        loadPage(++page)
                        println(page)
                    }
                }
            }
        })
    }

    fun loadPage(page: Int, vararg tags: String) {
        isLoadingView = true
        currentTags += tags
        runAsync(context, { runAsync { pages[page + 1] = Api.getPosts(page + 1, *currentTags.toTypedArray()) };getOrDownloadPage(page, *tags) }, { posts ->
            var finishedCount = 0
            var i = dataSet.size

            for (t in 0 until posts.size)
                dataSet.add(null)
            adapter.notifyItemRangeInserted(i - 1, posts.size)
            isLoadingView = false
            for (post in posts) {
                val index = i++
                runAsync(context, { Api.downloadImage(post.filePreviewURL, "${post.id}Preview") }) {
                    dataSet[index] = post
                    adapter.notifyItemChanged(index)
                    finishedCount++
                }
            }
        })
    }

    private suspend fun getOrDownloadPage(page: Int, vararg tags: String): List<Post> {
        var p = pages[page]
        if (p == null) p = Api.getPosts(page, *tags)
        if (dataSet.isNotEmpty()) {
            val lastFromLastPage = dataSet.last()
            val samePost = p.find { it.id == lastFromLastPage!!.id }
            if (samePost != null)
                p.takeWhile { println(it);it.id != samePost.id }
        }
        return p
    }

    fun reloadView() {
        dataSet.clear()
        adapter.notifyDataSetChanged()
        loadPage(1, *currentTags.toTypedArray())
    }

    fun clearView() {
        dataSet.clear()
        pages.clear()
        adapter.notifyDataSetChanged()
        currentTags.clear()
        loadPage(1)
    }


    private inner class Adapter : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.recycle_view_grid, parent, false) as ImageView))
        override fun getItemCount(): Int = dataSet.size
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val post = dataSet[position]
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
}


private class MyViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)