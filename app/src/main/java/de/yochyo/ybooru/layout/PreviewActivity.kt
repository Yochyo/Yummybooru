package de.yochyo.ybooru.layout

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Api
import de.yochyo.ybooru.api.Post
import de.yochyo.ybooru.file.FileManager
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.*
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*
import kotlinx.coroutines.*

class PreviewActivity : AppCompatActivity() {
    companion object {
        fun startActivity(context: Context, tags: String) = context.startActivity(Intent(context, PreviewActivity::class.java).apply { putExtra("tags", tags) })
    }

    private var root = SupervisorJob()
    private lateinit var m: Manager
    private lateinit var tags: Array<String>

    private var isLoadingView = false

    private lateinit var layoutManager: GridLayoutManager
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        setSupportActionBar(toolbar)
        val tagString = intent.getStringExtra("tags")
        tags = intent.getStringExtra("tags").toTagArray()
        m = Manager.getOrInit(tagString)

        recycler_view.layoutManager = GridLayoutManager(this, 3).apply { layoutManager = this }
        recycler_view.adapter = Adapter().apply { adapter = this }

        initScrollView()
        initSwipeRefreshLayout()
        loadPage(1)
    }

    fun loadPage(page: Int) {
        isLoadingView = true
        addChild(root) {
            val posts = getOrDownloadPage(page, *tags)

            launch(Dispatchers.Main) {
                var i = m.dataSet.size

                for (t in 0 until posts.size)
                    m.dataSet.add(posts[t])
                adapter.notifyItemRangeInserted(i - 1, posts.size)
                isLoadingView = false

                addChild(root, isAsync = true) {
                    for (post in posts) {
                        val index = i++
                        Api.downloadImage(this@PreviewActivity, post.filePreviewURL, preview(post.id))
                        withContext(Dispatchers.Main) { adapter.notifyItemChanged(index) }
                    }
                }
            }
            launch {
                if (m.pages[page + 1] == null) m.pages[page + 1] = Api.getPosts(this@PreviewActivity, page + 1, *tags)
            }
        }
    }

    private suspend fun getOrDownloadPage(page: Int, vararg tags: String): List<Post> {
        var p = m.pages[page]
        if (p == null) p = Api.getPosts(this, page, *tags)
        if (m.dataSet.isNotEmpty()) {
            val lastFromLastPage = m.dataSet.last()
            val samePost = p.find { it.id == lastFromLastPage.id }
            if (samePost != null)
                p.takeWhile { it.id != samePost.id }
        }
        return p
    }


    private fun initScrollView() {
        recycler_view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!isLoadingView)
                    if (layoutManager.findLastVisibleItemPosition() + 1 >= m.dataSet.size) loadPage(++m.currentPage)
            }
        })
    }

    private fun initSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            reloadView()
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.download_all -> {
                val builder = AlertDialog.Builder(this)
                val layout = LayoutInflater.from(this).inflate(R.layout.download_pictures_dialog_view, null) as LinearLayout
                builder.setView(layout)
                val dialog = builder.create()
                dialog.show()
                layout.findViewById<Button>(R.id.download_all_visible).setOnClickListener {
                    GlobalScope.launch {
                        for (p in m.dataSet)
                            FileManager.writeFile(p, Api.downloadImage(this@PreviewActivity, p.fileLargeURL, large(p.id), false))
                        withContext(Dispatchers.Main) { Toast.makeText(this@PreviewActivity, "Downloaded every picture", Toast.LENGTH_SHORT).show() }//TODO Notification
                    }
                    dialog.dismiss()
                }
                layout.findViewById<Button>(R.id.download_all_from_tags).setOnClickListener {
                    dialog.dismiss()
                    val b = AlertDialog.Builder(this@PreviewActivity)
                    b.setTitle("Download")
                    b.setMessage("Do you want to download everything that could be on this page?\n(You should not use this when using no tags or tags with many pictures)")//TODO einfach alle posts laden, bis die gesamtdateilänge mehr als der freie speicher ist oder alle posts da sind
                    b.setPositiveButton("Yes") { _, _ -> TODO() }//download all
                    b.setNegativeButton("No") { _, _ -> }
                    val d = b.create()
                    d.show()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.preview_menu, menu)
        return true
    }

    private inner class Adapter : RecyclerView.Adapter<MyViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder = MyViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.preview_image_view, parent, false) as ImageView)).apply {
            imageView.setOnClickListener {
                m.position = layoutPosition
                PictureActivity.startActivity(this@PreviewActivity, tags.toTagString())
            }
        }

        override fun getItemCount(): Int = m.dataSet.size
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val post = m.dataSet[position]
            if (post != null) {
                val bitmap = this@PreviewActivity.cache.getCachedBitmap(preview(post.id))
                if (bitmap != null)
                    holder.imageView.setImageBitmap(bitmap)
            } else holder.imageView.setImageDrawable(null)
        }
    }

    private inner class MyViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
}
