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
import de.yochyo.ybooru.api.downloadImage
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.FileManager
import de.yochyo.ybooru.utils.large
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
    }

    open fun loadPage(page: Int) {
        isLoadingView = true
        GlobalScope.launch {
            val i = m.dataSet.size
            val posts = m.getPage(page)
            launch(Dispatchers.Main) {
                previewAdapter.notifyItemRangeInserted(if (i > 0) i - 1 else 0, posts.size)
                isLoadingView = false
            }
            launch {
                m.downloadPage(m.currentPage + 1)
            }
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
                    if (layoutManager.findLastVisibleItemPosition() + 1 >= m.dataSet.size) loadPage(m.currentPage + 1)
                return super.onScrollStateChanged(recyclerView, newState)
            }
        })
    }
    fun reloadView() {
        m.reset()
        previewAdapter.notifyDataSetChanged()
        loadPage(1)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.download_all -> {
                val builder = AlertDialog.Builder(this)
                val layout = LayoutInflater.from(this).inflate(R.layout.download_pictures_dialog_view, null) as LinearLayout
                builder.setView(layout)
                val dialog = builder.create()
                dialog.show()
                layout.findViewById<Button>(R.id.download_all_visible).setOnClickListener {
                    dialog.dismiss()
                    Toast.makeText(this@PreviewActivity, "Download All visible pictures", Toast.LENGTH_SHORT).show()
                    for (p in m.dataSet)
                        downloadImage(p.fileLargeURL, large(p.id), {
                            FileManager.writeFile(p, it)
                            //TODO notification
                        }, false)
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

    override fun onResume() {
        super.onResume()
        layoutManager.scrollToPosition(m.position)
    }

    protected inner class PreviewAdapter : RecyclerView.Adapter<PreviewViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewViewHolder = PreviewViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.preview_image_view, parent, false) as ImageView)).apply {
            imageView.setOnClickListener {
                m.position = layoutPosition
                PictureActivity.startActivity(this@PreviewActivity, m.tags.toTagString())
            }
        }

        override fun getItemCount(): Int = m.dataSet.size
        override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {

        }

        override fun onViewDetachedFromWindow(holder: PreviewViewHolder) {
            super.onViewDetachedFromWindow(holder)
            holder.imageView.setImageBitmap(null)
        }

        override fun onViewAttachedToWindow(holder: PreviewViewHolder) {
            val pos = holder.adapterPosition
            if (pos != -1) {
                val p = m.dataSet[holder.adapterPosition]
                downloadImage(p.filePreviewURL, preview(p.id), {
                    if (pos == holder.adapterPosition)
                        holder.imageView.setImageBitmap(it)
                }, isScrolling)
            }
        }
    }

    protected inner class PreviewViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)


}
