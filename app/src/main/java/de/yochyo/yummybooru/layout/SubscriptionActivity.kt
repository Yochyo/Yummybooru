package de.yochyo.yummybooru.layout

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
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.UpdateSubsEvent
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.utils.setColor
import de.yochyo.yummybooru.utils.underline
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.content_subscription.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class SubscriptionActivity : AppCompatActivity() {
    private lateinit var totalTextView: TextView
    private lateinit var listener: Listener<UpdateSubsEvent>
    private var onClickedData: SubData? = null
    private lateinit var adapter: SubscribedTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
        setSupportActionBar(toolbar_subs)
        GlobalScope.launch(Dispatchers.Main) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            Manager.resetAll()
            initEverySubLayout()
            val recyclerView = subs_recycler
            recyclerView.layoutManager = LinearLayoutManager(this@SubscriptionActivity)
            recyclerView.adapter = SubscribedTagAdapter().apply { adapter = this }
            listener = UpdateSubsEvent.registerListener { adapter.updateSubs(); true }
            adapter.updateSubs()
            subs_swipe_refresh_layout.setOnRefreshListener {
                subs_swipe_refresh_layout.isRefreshing = false
                clear()
                reload()
            }
        }
    }

    private fun initEverySubLayout() {
        val layout = every_sub_item as LinearLayout
        layout.findViewById<TextView>(android.R.id.text1).text = "Coming Soon"
        totalTextView = layout.findViewById(android.R.id.text2)
    }

    override fun onDestroy() {
        super.onDestroy()
        UpdateSubsEvent.removeListener(listener)
        GlobalScope.launch { Manager.resetAll() }
    }

    private fun reload() {
        adapter.notifyDataSetChanged()
    }

    private fun clear() {
        onClickedData = null
    }

    override fun onResume() {
        super.onResume()
        if (onClickedData != null) {
            val sub = db.subs[onClickedData!!.clickedSub]
            if (!Manager.getOrInit(sub.toString()).posts.isEmpty()) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.save).setMessage(R.string.update_last_id)
                builder.setNegativeButton(R.string.no) { _, _ -> }
                builder.setPositiveButton(R.string.yes) { _, _ ->
                    GlobalScope.launch(Dispatchers.Main) {
                        db.changeSubscription(this@SubscriptionActivity, sub.copy(lastID = onClickedData!!.idWhenClicked, lastCount = onClickedData!!.countWhenClicked))
                        onClickedData = null
                    }
                }
                builder.show()
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
                            launch(Dispatchers.Main) {
                                val newTag: Tag = tag ?: Tag(it.text.toString(), Tag.UNKNOWN)
                                db.addSubscription(this@SubscriptionActivity, Subscription.fromTag(newTag))
                            }
                        }
                    }
                }.withTitle(getString(R.string.add_subscription)).build(this)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private inner class SubscribedTagAdapter : RecyclerView.Adapter<SubscribedTagViewHolder>() {
        fun updateSubs() {
            reload()
        }

        private fun longClickDialog(sub: Subscription) {
            val builder = AlertDialog.Builder(this@SubscriptionActivity)
            val array = arrayOf(if (sub.isFavorite) getString(R.string.unfavorite) else getString(R.string.set_favorite), getString(R.string.delete))
            builder.setItems(array) { dialog, i ->
                dialog.cancel()
                when (i) {
                    0 -> GlobalScope.launch { db.changeSubscription(this@SubscriptionActivity, sub.copy(isFavorite = !sub.isFavorite)) }
                    1 -> deleteSubDialog(sub)
                }

            }
            builder.show()
        }

        private fun deleteSubDialog(sub: Subscription) {
            val b = AlertDialog.Builder(this@SubscriptionActivity)
            b.setTitle(R.string.delete)
            b.setMessage("${getString(R.string.delete)} ${getString(R.string.subscription)} ${sub.name}?")
            b.setNegativeButton(R.string.no) { _, _ -> }
            b.setPositiveButton(R.string.yes) { _, _ -> GlobalScope.launch { db.deleteSubscription(this@SubscriptionActivity, sub.name) } }
            b.show()
        }

        override fun getItemCount(): Int = db.subs.size

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): SubscribedTagViewHolder = SubscribedTagViewHolder(layoutInflater.inflate(R.layout.subscription_item_layout, parent, false) as LinearLayout).apply {
            layout.setOnClickListener {
                val sub = db.subs.elementAt(adapterPosition)
                GlobalScope.launch {
                    onClickedData = SubData(adapterPosition, Api.newestID(), Api.getTag(sub.name)?.count ?: 0)
                }
                PreviewActivity.startActivity(this@SubscriptionActivity, sub.toString())
            }
            layout.setOnLongClickListener {
                val sub = db.subs.elementAt(adapterPosition)
                longClickDialog(sub)
                true
            }
        }

        override fun onBindViewHolder(holder: SubscribedTagViewHolder, position: Int) {
            val text1 = holder.layout.findViewById<TextView>(android.R.id.text1)
            val text2 = holder.layout.findViewById<TextView>(android.R.id.text2)
            val sub = db.subs.elementAt(position)
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

private class SubData(val clickedSub: Int, val idWhenClicked: Int, val countWhenClicked: Int)
