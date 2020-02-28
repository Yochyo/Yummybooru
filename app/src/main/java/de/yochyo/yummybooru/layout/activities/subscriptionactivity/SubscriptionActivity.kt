package de.yochyo.yummybooru.layout.activities.subscriptionactivity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventcollection.SubEventCollection
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.UpdateSubsEvent
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.content_subscription.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.collections.ArrayList

class SubscriptionActivity : AppCompatActivity() {
    private val filteredSubs = ArrayList<Pair<EventCollection<Subscription>, String>>()
        get() {
            if (field.isEmpty()) field += Pair(db.subs, "")
            return field
        }
    val currentFilter: EventCollection<Subscription> get() = filteredSubs.last().first

    private val updateSubsListener = Listener.create<UpdateSubsEvent> { adapter.notifyDataSetChanged() }
    var onClickedData: SubData? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubscribedTagAdapter
    lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
        setSupportActionBar(toolbar_subs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        recyclerView = subs_recycler
        recyclerView.layoutManager = LinearLayoutManager(this@SubscriptionActivity).apply { layoutManager = this }

        recyclerView.adapter = SubscribedTagAdapter(this).apply { adapter = this;util.adapter = this }
        sub_filter.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) GlobalScope.launch { filter(newText) }
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })
        UpdateSubsEvent.registerListener(updateSubsListener)
        subs_swipe_refresh_layout.setOnRefreshListener {
            subs_swipe_refresh_layout.isRefreshing = false
            clear()
            adapter.notifyDataSetChanged()
        }
    }

    private fun clear() {
        onClickedData = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.subscription_menu, menu)
        return true
    }

    private val filterMutex = Mutex()
    suspend fun filter(name: String) {
        withContext(Dispatchers.Default) {
            filterMutex.withLock {
                var addChild: EventCollection<Subscription>
                if (name == "") {
                    for (item in filteredSubs) {
                        val list = item.first
                        if (list is SubEventCollection) list.destroy()
                    }
                    filteredSubs.clear()
                    addChild = db.subs
                } else {
                    for (i in filteredSubs.indices.reversed()) {
                        if (name.startsWith(filteredSubs[i].second)) {
                            val subCollection = SubEventCollection(TreeSet(), filteredSubs[i].first) { it.name.contains(name) }
                            addChild = subCollection
                            break
                        }
                    }
                    addChild = SubEventCollection(TreeSet(), db.subs) { it.name.contains(name) }
                }
                withContext(Dispatchers.Main) {
                    layoutManager.scrollToPosition(0)
                    adapter.notifyDataSetChanged()
                    filteredSubs += Pair(addChild, name)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.util.paused = false
        if (onClickedData != null) {
            val sub = currentFilter[onClickedData!!.clickedSub]
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.save).setMessage(R.string.update_last_id)
            builder.setNegativeButton(R.string.no) { _, _ -> }
            builder.setPositiveButton(R.string.yes) { _, _ ->
                GlobalScope.launch(Dispatchers.Main) {
                    db.changeSubscription(sub.copy(lastID = onClickedData!!.idWhenClicked, lastCount = onClickedData!!.countWhenClicked))
                    onClickedData = null
                }
            }
            builder.show()
        }
    }

    override fun onPause() {
        super.onPause()
        adapter.util.paused = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.add_subscription -> {
                AddTagDialog {
                    if (db.getSubscription(it.text.toString()) == null) {
                        GlobalScope.launch {
                            val tag = Api.getTag(this@SubscriptionActivity, it.text.toString())
                            val sub = Subscription.fromTag(this@SubscriptionActivity, tag)
                            db.addSubscription(sub)
                            withContext(Dispatchers.Main) {
                                layoutManager.scrollToPositionWithOffset(currentFilter.indexOf(sub), 0)
                            }
                        }
                    }
                }.withTitle(getString(R.string.add_subscription)).build(this)
            }
            R.id.update_subs -> {
                ConfirmDialog {
                    GlobalScope.launch {
                        val id = Api.newestID(this@SubscriptionActivity)
                        for (sub in currentFilter) {
                            val tag = Api.getTag(this@SubscriptionActivity, sub.name)
                            db.changeSubscription(sub.copy(lastCount = tag.count, lastID = id))
                        }
                    }
                }.withTitle("Update all subs?").build(this@SubscriptionActivity)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        UpdateSubsEvent.removeListener(updateSubsListener)
        adapter.util.close()
    }



}

class SubData(val clickedSub: Int, val idWhenClicked: Int, val countWhenClicked: Int)
