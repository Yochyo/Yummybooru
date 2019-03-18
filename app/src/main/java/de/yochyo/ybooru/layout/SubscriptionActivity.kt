package de.yochyo.ybooru.layout

import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Api
import de.yochyo.ybooru.database
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.addChild
import de.yochyo.ybooru.utils.setColor
import de.yochyo.ybooru.utils.underline
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.content_subscription.*
import kotlinx.coroutines.*

class SubscriptionActivity : AppCompatActivity() {
    private var root = SupervisorJob()
    private var clickedSub: Int? = null
    private var newestIdWhenClicked: Int? = null
    private lateinit var adapter: SubscribedTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Manager.resetAll()
        setContentView(R.layout.activity_subscription)
        setSupportActionBar(toolbar_subs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val recyclerView = subs_recycler
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SubscribedTagAdapter().apply { adapter = this }
        subs_swipe_refresh_layout.setOnRefreshListener {
            subs_swipe_refresh_layout.isRefreshing = false
            root.cancelChildren()
            root = SupervisorJob()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Manager.resetAll()
        root.cancelChildren()
    }

    override fun onResume() {
        adapter.notifyDataSetChanged()
        super.onResume()
        if (clickedSub != null) {
            val pos = clickedSub!!
            clickedSub = null
            val sub = database.getSubscriptions().elementAt(pos)
            if (Manager.getOrInit(sub.tagString).dataSet.isNotEmpty()) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Save").setMessage("Update last id?")
                builder.setNegativeButton("No") { _, _ -> }
                builder.setPositiveButton("Yes") { _, _ ->
                    GlobalScope.launch {
                        sub.lastID = sub.currentID
                        if (newestIdWhenClicked == null)
                            sub.currentID = Api.newestID(this@SubscriptionActivity)
                        else {
                            sub.currentID = newestIdWhenClicked!!
                            newestIdWhenClicked = null
                        }
                        database.changeSubscription(sub)
                        GlobalScope.launch(Dispatchers.Main) { adapter.notifyItemChanged(pos) }
                    }
                }
                builder.create().show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class SubscribedTagAdapter : RecyclerView.Adapter<SubscribedTagViewHolder>() {
        override fun getItemCount(): Int = database.getSubscriptions().size

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): SubscribedTagViewHolder = SubscribedTagViewHolder(LayoutInflater.from(this@SubscriptionActivity).inflate(R.layout.subscription_item_layout, parent, false) as LinearLayout).apply {
            layout.setOnClickListener {
                val sub = database.getSubscriptions().elementAt(adapterPosition)
                clickedSub = adapterPosition
                GlobalScope.launch { newestIdWhenClicked = Api.newestID(this@SubscriptionActivity) }
                PreviewActivity.startActivity(this@SubscriptionActivity, sub.tagString)
            }
            layout.setOnLongClickListener {
                val builder = AlertDialog.Builder(this@SubscriptionActivity)
                builder.setTitle("Delete").setMessage("Delete sub?")
                builder.setNegativeButton("No") { _, _ -> }
                builder.setPositiveButton("Yes") { _, _ ->
                    val sub = database.getSubscriptions().elementAt(adapterPosition)
                    database.removeSubscription(sub.tag.name)
                    adapter.notifyItemRemoved(adapterPosition)
                }
                builder.create().show()
                true
            }
        }

        override fun onBindViewHolder(holder: SubscribedTagViewHolder, position: Int) {
            val text1 = holder.layout.findViewById<TextView>(android.R.id.text1)
            val text2 = holder.layout.findViewById<TextView>(android.R.id.text2)
            val sub = database.getSubscriptions().elementAt(position)
            text1.text = sub.tag.name
            text1.setColor(sub.tag)
            text1.underline(sub.tag.isFavorite)
            if (sub.currentID == 0) text2.text = "Number of new pictures: ~"
            else {
                text2.text = "Number of new pictures: "
                addChild(root) {
                    val pos = holder.adapterPosition
                    val m = Manager.getOrInit(sub.tagString)
                    var oldestID = Int.MAX_VALUE
                    var currentPage = 0
                    var posts = 0
                    try {
                        while (oldestID > sub.currentID && isActive) {
                            val page = m.downloadPage(this@SubscriptionActivity, ++currentPage)
                            posts += page.size
                            oldestID = page.last().id
                        }
                    } catch (e: Exception) {
                    }
                    if (holder.adapterPosition == pos) launch(Dispatchers.Main) { text2.text = "Number of new pictures: $posts" }
                }
            }
        }
    }

    private inner class SubscribedTagViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)
}
