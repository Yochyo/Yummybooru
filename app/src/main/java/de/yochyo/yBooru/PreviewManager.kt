package de.yochyo.yBooru

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import de.yochyo.danbooruAPI.Api
import de.yochyo.yBooru.api.Post
import de.yochyo.yBooru.utils.addChild
import kotlinx.coroutines.*

class PreviewManager(private val context: Context, val view: RecyclerView, val tags: Array<String> = arrayOf()) {
    var root = SupervisorJob()

    private val pages = HashMap<Int, List<Post>>() //page, posts
    private var page = 1

    private var isLoadingView = false

    private val layoutManager = GridLayoutManager(context, 3)
    private val dataSet = ArrayList<Post?>(200)
    private val adapter = Adapter()

    init {
        view.layoutManager = layoutManager
        view.adapter = adapter
        scrollView()
    }

    fun loadPage(page: Int) {
        isLoadingView = true
        addChild(root) {
            async { pages[page + 1] = Api.getPosts(page + 1, *tags) }

            val posts = getOrDownloadPage(page, *tags)

            launch(Dispatchers.Main) {
                var finishedCount = 0
                var i = dataSet.size

                for (t in 0 until posts.size)
                    dataSet.add(null)
                adapter.notifyItemRangeInserted(i - 1, posts.size)
                isLoadingView = false
                for (post in posts) {
                    val index = i++
                    addChild(root, isAsync = true) {//TODO deaktivieren um daten zu sparen
                        withContext(Dispatchers.Default) { Api.downloadImage(post.filePreviewURL, "${post.id}Preview") }
                        if (isActive) {
                            dataSet[index] = post
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
        dataSet.clear()
        pages.clear()
        adapter.notifyDataSetChanged()
        loadPage(1)
    }

    fun clearView() {
        reloadView()
    }


    private fun scrollView() {
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

    private suspend fun getOrDownloadPage(page: Int, vararg tags: String): List<Post> {
        var p = pages[page]
        if (p == null) p = Api.getPosts(page, *tags)
        if (dataSet.isNotEmpty()) {
            val lastFromLastPage = dataSet.last()
            if(lastFromLastPage != null){
                val samePost = p.find { it.id == lastFromLastPage.id }
                if (samePost != null)
                    p.takeWhile { println(it);it.id != samePost.id }
            }
        }
        return p
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