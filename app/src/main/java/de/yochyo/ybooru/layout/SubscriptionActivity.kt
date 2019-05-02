package de.yochyo.ybooru.layout

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.api.Api
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.database.entities.Subscription
import de.yochyo.ybooru.database.entities.Tag
import de.yochyo.ybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.setColor
import de.yochyo.ybooru.utils.underline
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.content_subscription.*
import kotlinx.coroutines.*
import java.util.*

class SubscriptionActivity : AppCompatActivity() {
    private val observer = Observer<TreeSet<Subscription>> { t -> if (t != null) adapter.updateSubs(t) }
    private var clickedSub: Int? = null
    private var whenClicked: Pair<Int, Int>? = null //ID, count
    private lateinit var adapter: SubscribedTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
        setSupportActionBar(toolbar_subs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        Manager.resetAll()
        val recyclerView = subs_recycler
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SubscribedTagAdapter().apply { adapter = this }
        db.subs.observe(this, observer)
        subs_swipe_refresh_layout.setOnRefreshListener {
            subs_swipe_refresh_layout.isRefreshing = false
            clear()
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Manager.resetAll()
        db.subs.removeObserver(observer)
    }

    private fun clear() {
        whenClicked = null
        clickedSub = null
    }

    override fun onResume() {
        super.onResume()
        if (clickedSub != null) {
            val pos = clickedSub!!
            clickedSub = null
            val sub = db.subs[pos]
            if (!Manager.getOrInit(sub.toString()).posts.isEmpty) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.save).setMessage(R.string.update_last_id)
                builder.setNegativeButton(R.string.no) { _, _ -> }
                builder.setPositiveButton(R.string.yes) { _, _ ->
                    GlobalScope.launch {
                        if (whenClicked == null) {
                            sub.lastID = Api.newestID()
                            sub.lastCount = Api.getTag(sub.name)?.count ?: 0
                        } else {
                            sub.lastID = whenClicked!!.first
                            sub.lastCount = whenClicked!!.second
                            whenClicked == null
                        }
                        launch(Dispatchers.Main) { db.changeSubscription(sub) }
                    }
                }
                builder.create().show()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.subscription_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.add_subscription -> {
                AddTagDialog {
                    if (db.getSubscription(it.text.toString()) == null) {
                        GlobalScope.launch {
                            val tag = Api.getTag(it.text.toString())
                            val newest = Api.newestID()
                            launch(Dispatchers.Main) {
                                val newTag: Tag = tag ?: Tag(it.text.toString(), Tag.UNKNOWN)
                                db.addSubscription(Subscription(newTag.name, newTag.type, newest, newTag.count))
                            }
                        }
                    }
                }.apply { title = getString(R.string.add_subscription) }.build(this)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class SubscribedTagAdapter : RecyclerView.Adapter<SubscribedTagViewHolder>() {
        private var subs = TreeSet<Subscription>()

        fun updateSubs(subs: TreeSet<Subscription>) {
            this.subs = subs
            println(subs.joinToString { it.name })
            notifyDataSetChanged()
        }

        private fun longClickDialog(sub: Subscription) {
            val builder = AlertDialog.Builder(this@SubscriptionActivity)
            val array: Array<String>
            if (sub.isFavorite) array = arrayOf(getString(R.string.unfavorite), getString(R.string.delete))
            else array = arrayOf(getString(R.string.set_favorite), getString(R.string.delete))
            builder.setItems(array) { dialog, i ->
                dialog.cancel()
                when (i) {
                    0 -> {
                        db.changeSubscription(sub.copy(isFavorite = !sub.isFavorite))
                        Toast.makeText(this@SubscriptionActivity, "${if (sub.isFavorite) getString(R.string.favorite) else getString(R.string.unfavorite)} <${sub.name}>", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        deleteSubDialog(sub)
                    }
                }

            }
            builder.show()
        }

        private fun deleteSubDialog(sub: Subscription) {
            val b = AlertDialog.Builder(this@SubscriptionActivity)
            b.setTitle(R.string.delete)
            b.setMessage("${getString(R.string.delete)} ${getString(R.string.subscription)} ${sub.name}?")
            b.setNegativeButton(R.string.no) { _, _ -> }
            b.setPositiveButton(R.string.yes) { _, _ ->
                db.deleteSubscription(sub.name)
                Toast.makeText(this@SubscriptionActivity, "${getString(R.string.deleted)} [${sub.name}]", Toast.LENGTH_SHORT).show()
            }
            b.show()
        }

        override fun getItemCount(): Int = subs.size

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): SubscribedTagViewHolder = SubscribedTagViewHolder(layoutInflater.inflate(R.layout.subscription_item_layout, parent, false) as LinearLayout).apply {
            layout.setOnClickListener {
                val sub = subs.elementAt(adapterPosition)
                clickedSub = adapterPosition
                GlobalScope.launch {
                    whenClicked = Pair(Api.newestID(), Api.getTag(sub.name)?.count ?: 0)
                } //TODO was ist, wenn der download erst fertig ist, wenn die activity wieder resumed wurde
                PreviewActivity.startActivity(this@SubscriptionActivity, sub.toString())
            }
            layout.setOnLongClickListener {
                val sub = subs.elementAt(adapterPosition)
                longClickDialog(sub)
                true
            }
        }

        override fun onBindViewHolder(holder: SubscribedTagViewHolder, position: Int) {
            val text1 = holder.layout.findViewById<TextView>(android.R.id.text1)
            val text2 = holder.layout.findViewById<TextView>(android.R.id.text2)
            val sub = subs.elementAt(position)
            text1.text = sub.name
            text1.setColor(sub.color)
            text1.underline(sub.isFavorite)
            text2.text = getString(R.string.number_of_new_pictures)
            GlobalScope.launch {
                var tag = Api.getTag(sub.name)
                if (tag == null) tag = Tag(sub.name, sub.type)
                val countDifference = tag.count - sub.lastCount
                launch(Dispatchers.Main) {
                    text2.text = "${getString(R.string.number_of_new_pictures)}$countDifference"
                }
            }
        }
    }

    private inner class SubscribedTagViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)
}
