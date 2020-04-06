package de.yochyo.yummybooru.layout.activities.subscriptionactivity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventcollection.SubEventCollection
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventcollection.observablecollection.ObservingSubEventCollection
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Sub
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.utils.general.FilteringEventCollection
import de.yochyo.yummybooru.utils.general.createTagAndOrChangeSubState
import de.yochyo.yummybooru.utils.general.currentServer
import kotlinx.android.synthetic.main.activity_subscription.*
import kotlinx.android.synthetic.main.content_subscription.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class SubscriptionActivity : AppCompatActivity() {
    val filteringSubList = FilteringEventCollection({ ObservingSubEventCollection(TreeSet(), db.tags) { it.sub != null } }, { it.name })
    suspend fun filter(name: String) {
        val result = filteringSubList.filter(name)
        withContext(Dispatchers.Main) {
            layoutManager.scrollToPosition(0)
            adapter.updateSubs(result)
        }
    }

    private val updateSubsListener = Listener.create<OnUpdateEvent<Tag>> { println(it.collection.joinToString { it.name });GlobalScope.launch(Dispatchers.Main) { adapter.updateSubs(filteringSubList) } }
    var onClickedData: SubData? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SubscribedTagAdapter
    lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
        setSupportActionBar(toolbar_subs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val subsC = ObservingSubEventCollection(TreeSet(), db.tags) { it.sub != null }
        subsC.registerOnElementChangeListener { println("new: ${it.new.name}, arg: ${it.arg}") }
        recyclerView = subs_recycler
        recyclerView.layoutManager = LinearLayoutManager(this@SubscriptionActivity).apply { layoutManager = this }

        recyclerView.adapter = SubscribedTagAdapter(this, filteringSubList).apply { adapter = this;util.adapter = this }
        sub_filter.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) GlobalScope.launch { filter(newText) }
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })
        db.tags.registerOnUpdateListener(updateSubsListener)
        subs_swipe_refresh_layout.setOnRefreshListener {
            subs_swipe_refresh_layout.isRefreshing = false
            clear()
            adapter.updateSubs(filteringSubList)
        }
    }

    private fun clear() {
        onClickedData = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.subscription_menu, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        adapter.util.paused = false
        val clickedData = onClickedData
        if (clickedData != null) {

            val tag = filteringSubList.find { it.name == clickedData.name }
            if(tag != null){
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.save).setMessage(R.string.update_last_id)
                builder.setNegativeButton(R.string.no) { _, _ -> }
                builder.setPositiveButton(R.string.yes) { _, _ ->
                    GlobalScope.launch(Dispatchers.Main) {
                        tag.sub = Sub(clickedData.idWhenClicked, clickedData.countWhenClicked)
                        onClickedData = null
                    }
                }
                builder.show()
            }
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
                    GlobalScope.launch {
                        val sub = createTagAndOrChangeSubState(this@SubscriptionActivity, it.text.toString())
                        withContext(Dispatchers.Main) {
                            layoutManager.scrollToPositionWithOffset(filteringSubList.indexOf(sub), 0)
                        }
                    }
                }.withTitle(getString(R.string.add_subscription)).build(this)
            }
            R.id.update_subs -> {
                ConfirmDialog {
                    GlobalScope.launch {
                        val id = currentServer.newestID()
                        if (id != null) {
                            for (sub in filteringSubList) {
                                val tag = currentServer.getTag(sub.name)
                                if (tag != null) {
                                    val tagInDb = db.getTag(tag.name)
                                    tagInDb?.sub = Sub(id, tag.count)
                                }
                            }
                        }
                    }
                }.withTitle("Update all subs?").build(this@SubscriptionActivity)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        db.tags.removeOnUpdateListener(updateSubsListener)
        adapter.util.close()
    }


}

class SubData(val name: String, val idWhenClicked: Int, val countWhenClicked: Int)
