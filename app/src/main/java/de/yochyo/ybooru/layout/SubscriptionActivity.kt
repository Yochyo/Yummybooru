package de.yochyo.ybooru.layout

import android.os.Build
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
import de.yochyo.ybooru.api.Subscription
import de.yochyo.ybooru.database
import de.yochyo.ybooru.manager.Manager
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.content_subscription.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SubscriptionActivity : AppCompatActivity() {
    private var clickedSub: Int? = null
    private val subs = ArrayList<Subscription>()
    private lateinit var adapter: SubscribedTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Manager.resetAll()
        setContentView(R.layout.activity_subscription)
        setSupportActionBar(toolbar_subs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        subs += database.getSubscriptions()
        val recyclerView = subs_recycler
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SubscribedTagAdapter().apply { adapter = this }
    }

    override fun onResume() {
        super.onResume()
        if (clickedSub != null) {
            val sub = subs[clickedSub!!]
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Save")
            builder.setMessage("Update last id?")
            builder.setNegativeButton("No") { _, _ -> }
            builder.setPositiveButton("Yes") { _, _ ->
                GlobalScope.launch {
                    sub.lastID = sub.currentID
                    sub.currentID = Api.newestID(this@SubscriptionActivity)
                    database.changeSubscription(sub)
                    GlobalScope.launch(Dispatchers.Main) {
                        adapter.notifyItemChanged(clickedSub!!)
                        clickedSub = null
                    }
                }
            }
            builder.create().show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Manager.resetAll()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class SubscribedTagAdapter : RecyclerView.Adapter<SubscribedTagViewHolder>() {
        override fun getItemCount(): Int = subs.size

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): SubscribedTagViewHolder = SubscribedTagViewHolder(LayoutInflater.from(this@SubscriptionActivity).inflate(R.layout.subscription_item_layout, parent, false) as LinearLayout).apply {
            layout.setOnClickListener {
                val sub = subs[adapterPosition]
                if (Manager.getOrInit(sub.tag.name).dataSet.isNotEmpty()) {
                    clickedSub = adapterPosition
                    SubscriptionPreviewActivity.startActivity(this@SubscriptionActivity, subs[adapterPosition].tag.name, sub.currentID)
                }
            }
            layout.setOnLongClickListener {
                val builder = AlertDialog.Builder(this@SubscriptionActivity)
                builder.setTitle("Delete")
                builder.setMessage("Delete sub?")
                builder.setNegativeButton("No") { _, _ -> }
                builder.setPositiveButton("Yes") { _, _ ->
                    val pos = adapterPosition
                    val sub = subs[pos]
                    subs.remove(sub)
                    adapter.notifyItemChanged(pos)
                    database.removeSubscription(sub.tag.name)
                }
                builder.create().show()
                true
            }
        }

        override fun onBindViewHolder(holder: SubscribedTagViewHolder, position: Int) {
            val text1 = holder.layout.findViewById<TextView>(android.R.id.text1)
            val text2 = holder.layout.findViewById<TextView>(android.R.id.text2)
            val sub = subs[position]
            text1.text = sub.tag.name
            if (Build.VERSION.SDK_INT > 22) text1.setTextColor(getColor(sub.tag.color))
            else text1.setTextColor(resources.getColor(sub.tag.color))
            if (sub.currentID == 0)
                text2.text = "Number of new pictures: ~"
            else {
                text2.text = "Number of new pictures: "
                GlobalScope.launch {
                    val pos = holder.adapterPosition
                    Manager.reset(sub.tag.name)
                    val m = Manager.getOrInit(sub.tag.name)
                    var oldestID = Int.MAX_VALUE
                    var currentPage = 0
                    var posts = 0
                    try {
                        while (oldestID > sub.currentID) {
                            val page = m.downloadPage(this@SubscriptionActivity, ++currentPage, idNewerThan = sub.currentID)
                            posts += page.size
                            oldestID = page.last().id
                        }
                    } catch (e: Exception) {
                    }
                    if (holder.adapterPosition == pos)
                        launch(Dispatchers.Main) { text2.text = "Number of new pictures: $posts" }
                }
            }
        }
    }

    private inner class SubscribedTagViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)
}
