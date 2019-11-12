package de.yochyo.yummybooru.layout

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
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
import de.yochyo.yummybooru.layout.res.Menus
import de.yochyo.yummybooru.layout.views.*
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.underline
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.content_subscription.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class SubscriptionActivity : AppCompatActivity() {
    private val filteredSubs = ArrayList<Pair<EventCollection<Subscription>, String>>()
        get() {
            if (field.isEmpty()) field += Pair(db.subs, "")
            return field
        }
    private val currentFilter: EventCollection<Subscription> get() = filteredSubs.last().first

    private val updateSubsListener = Listener.create<UpdateSubsEvent> { adapter.notifyDataSetChanged() }
    private var onClickedData: SubData? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubscribedTagAdapter
    private lateinit var layoutManager: LinearLayoutManager

    private val countWrapper = CountWrapper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
        setSupportActionBar(toolbar_subs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        recyclerView = subs_recycler
        recyclerView.layoutManager = LinearLayoutManager(this@SubscriptionActivity).apply { layoutManager = this }

        recyclerView.adapter = SubscribedTagAdapter().apply { adapter = this;countWrapper.adapter = this }
        sub_filter.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                println(newText)
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

    private inner class SubscribedTagAdapter : SelectableRecyclerViewAdapter<SubscribedTagViewHolder>(this, R.menu.subscription_activity_selection_menu) {
        private val startSelectionListener = Listener.create<StartSelectingEvent> {
            adapter.actionmode?.title = "${adapter.selected.size}/${currentFilter.size}"
        }
        private val updateSelectionListener = Listener.create<UpdateSelectionEvent> {
            adapter.actionmode?.title = "${selected.size}/${currentFilter.size}"
        }
        private val disableMenuClick = Listener.create<StartSelectingEvent> { notifyDataSetChanged() }
        private val reEnableMenuClick = Listener.create<StopSelectingEvent> { notifyDataSetChanged() }

        init {
            onStartSelection.registerListener(startSelectionListener)
            onUpdateSelection.registerListener(updateSelectionListener)
            onStartSelection.registerListener(disableMenuClick)
            onStopSelection.registerListener(reEnableMenuClick)

            onClickMenuItem.registerListener {
                when (it.menuItem.itemId) {
                    R.id.select_all -> if (adapter.selected.size == currentFilter.size) adapter.unselectAll() else adapter.selectAll()
                    R.id.open_selected -> {
                        Toast.makeText(this@SubscriptionActivity, "Not yet implemented", Toast.LENGTH_SHORT).show()
                        adapter.unselectAll()
                    }
                    R.id.update_subs -> {
                        ConfirmDialog {
                            GlobalScope.launch {
                                val id = Api.newestID()
                                for (selected in adapter.selected.getSelected(currentFilter)) {
                                    val tag = Api.getTag(selected.name)
                                    db.changeSubscription(this@SubscriptionActivity, selected.copy(lastCount = tag.count, lastID = id))
                                }
                            }
                        }.withTitle("Update selected subs?").build(this@SubscriptionActivity)
                    }
                }
            }
        }

        override val onClickLayout = { holder: SubscribedTagViewHolder ->
            val sub = currentFilter.elementAt(holder.adapterPosition)
            GlobalScope.launch {
                onClickedData = SubData(holder.adapterPosition, Api.newestID(), Api.getTag(sub.name).count)
            }
            PreviewActivity.startActivity(this@SubscriptionActivity, sub.toString())
        }


        override fun setListeners(holder: SubscribedTagViewHolder) {
            val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
            toolbar.setOnClickListener { onClickHolder(holder) }
            toolbar.setOnLongClickListener { onSelectHolder(holder); true }
        }


        override fun createViewHolder(parent: ViewGroup): SubscribedTagViewHolder {
            return SubscribedTagViewHolder(layoutInflater.inflate(R.layout.subscription_item_layout, parent, false) as FrameLayout)
        }

        private fun deleteSubDialog(sub: Subscription) {
            val b = AlertDialog.Builder(this@SubscriptionActivity)
            b.setTitle(R.string.delete)
            b.setMessage("${getString(R.string.delete)} ${getString(R.string.subscription)} ${sub.name}?")
            b.setNegativeButton(R.string.no) { _, _ -> }
            b.setPositiveButton(R.string.yes) { _, _ -> GlobalScope.launch { db.deleteSubscription(this@SubscriptionActivity, sub.name) } }
            b.show()
        }

        override fun getItemCount(): Int = currentFilter.size
        override fun onCreateViewHolder(parent: ViewGroup, position: Int): SubscribedTagViewHolder {
            val holder = super.onCreateViewHolder(parent, position)
            val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
            toolbar.inflateMenu(R.menu.activity_subscription_item_menu)
            toolbar.setOnMenuItemClickListener {
                val sub = currentFilter.elementAt(holder.adapterPosition)
                when (it.itemId) {
                    R.id.subscription_set_favorite -> GlobalScope.launch {
                        val copy = sub.copy(isFavorite = !sub.isFavorite)
                        db.changeSubscription(this@SubscriptionActivity, copy)
                        withContext(Dispatchers.Main) { layoutManager.scrollToPositionWithOffset(currentFilter.indexOf(copy), 0) }
                    }
                    R.id.subscription_delete -> deleteSubDialog(sub)
                }
                true
            }
            return holder
        }

        override fun onBindViewHolder(holder: SubscribedTagViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            val toolbar = holder.layout.findViewById<Toolbar>(R.id.toolbar)
            val text1 = toolbar.findViewById<TextView>(android.R.id.text1)
            val text2 = toolbar.findViewById<TextView>(android.R.id.text2)
            val sub = currentFilter.elementAt(position)
            text1.text = sub.name
            text1.setColor(sub.color)
            text1.underline(sub.isFavorite)
            text2.text = "${getString(R.string.number_of_new_pictures)} ${countWrapper.getCount(sub)}"
            Menus.initSubscriptionMenu(toolbar.menu, sub)
            toolbar.menu.setGroupEnabled(0, selected.isEmpty()) //Cannot access menu when selecting items
        }
    }


    override fun onResume() {
        super.onResume()
        countWrapper.paused = false
        if (onClickedData != null) {
            val sub = currentFilter[onClickedData!!.clickedSub]
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

    override fun onPause() {
        super.onPause()
        countWrapper.paused = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.add_subscription -> {
                AddTagDialog {
                    if (db.getSubscription(it.text.toString()) == null) {
                        GlobalScope.launch {
                            val tag = Api.getTag(it.text.toString())
                            val sub = Subscription.fromTag(tag)
                            db.addSubscription(this@SubscriptionActivity, sub)
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
                        val id = Api.newestID()
                        for (sub in currentFilter) {
                            val tag = Api.getTag(sub.name)
                            db.changeSubscription(this@SubscriptionActivity, sub.copy(lastCount = tag.count, lastID = id))
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
        countWrapper.close()
    }

    inner class SubscribedTagViewHolder(layout: FrameLayout) : SelectableViewHolder(layout)
    private inner class CountWrapper {
        var adapter: RecyclerView.Adapter<SubscribedTagViewHolder>? = null
        private val counts = HashMap<String, Int>()
        private val mutex = Any()

        var paused = false
        private val job = GlobalScope.launch(Dispatchers.IO) {
            var i = 0
            while (isActive) {
                try {
                    if (!paused) cacheCount(currentFilter[i++].name)
                    else delay(100)
                } catch (e: Exception) {
                    i = 0
                }
            }
        }

        private val onAddElementListener = Listener.create<EventCollection<Subscription>.OnAddElementEvent> { event -> GlobalScope.launch { cacheCount(event.element.name) } }

        init {
            db.subs.onAddElement.registerListener(onAddElementListener)
        }

        fun getCount(sub: Subscription): Int {
            GlobalScope.launch { cacheCount(sub.name) }
            val countDifference = (getRawCount(sub.name) ?: 0) - sub.lastCount
            return if (countDifference > 0) countDifference else 0
        }

        private suspend fun cacheCount(name: String) {
            withContext(Dispatchers.IO) {
                try {
                    val oldValue = getRawCount(name)
                    val tag = Api.getTag(name)
                    setCount(name, tag.count)
                    if (oldValue != tag.count) {
                        val newIndex = currentFilter.indexOfFirst { it.name == name }
                        if (newIndex >= 0)
                            withContext(Dispatchers.Main) { adapter?.notifyItemChanged(newIndex) }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun setCount(name: String, count: Int) {
            synchronized(mutex) {
                counts[name] = count
            }
        }

        private fun getRawCount(name: String): Int? {
            return synchronized(mutex) {
                counts[name]
            }
        }

        fun close() {
            db.subs.onAddElement.removeListener(onAddElementListener)
            job.cancel()
        }
    }
}

private class SubData(val clickedSub: Int, val idWhenClicked: Int, val countWhenClicked: Int)
