package de.yochyo.yummybooru.layout.activities.followingactivity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventcollection.observablecollection.ObservingSubEventCollection
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.utils.general.FilteringEventCollection
import de.yochyo.yummybooru.utils.general.createTagAndOrChangeFollowingState
import kotlinx.android.synthetic.main.activity_following.*
import kotlinx.android.synthetic.main.content_following.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class FollowingActivity : AppCompatActivity() {
    lateinit var filteringFollowingList: FilteringEventCollection<Tag, Int>
    suspend fun filter(name: String) {
        val result = filteringFollowingList.filter(name)
        withContext(Dispatchers.Main) {
            layoutManager.scrollToPosition(0)
            adapter.updateFollowing(result)
        }
    }

    private val updateFollowingListener = Listener<OnUpdateEvent<Tag>> { GlobalScope.launch(Dispatchers.Main) { adapter.updateFollowing(filteringFollowingList) } }
    var onClickedData: FollowingData? = null

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FollowingTagAdapter
    lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val oldName = savedInstanceState?.getString("name")
        val oldId = savedInstanceState?.getInt("id")
        val oldCount = savedInstanceState?.getInt("count")
        if (oldName != null && oldId != null && oldCount != null) onClickedData = FollowingData(oldName, oldId, oldCount)
        filteringFollowingList = FilteringEventCollection({ ObservingSubEventCollection(TreeSet(), db.tags) { it.following != null } }, { it.name })
        setContentView(R.layout.activity_following)
        setSupportActionBar(toolbar_following)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        recyclerView = following_recycler
        recyclerView.layoutManager = LinearLayoutManager(this@FollowingActivity).apply { layoutManager = this }

        recyclerView.adapter = FollowingTagAdapter(this, recyclerView, filteringFollowingList).apply { adapter = this;util.adapter = this }
        following_filter.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) GlobalScope.launch { filter(newText) }
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })
        db.tags.registerOnUpdateListener(updateFollowingListener)
        following_swipe_refresh_layout.setOnRefreshListener {
            following_swipe_refresh_layout.isRefreshing = false
            clear()
            adapter.updateFollowing(filteringFollowingList)
        }
    }

    private fun clear() {
        onClickedData = null
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.following_menu, menu)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val finalOnCLickedData = onClickedData
        if (finalOnCLickedData != null) {
            outState.putString("name", finalOnCLickedData.name)
            outState.putInt("id", finalOnCLickedData.idWhenClicked)
            outState.putInt("count", finalOnCLickedData.countWhenClicked)
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.util.paused = false
        val clickedData = onClickedData
        if (clickedData != null) {
            val tag = filteringFollowingList.find { it.name == clickedData.name }
            if (tag != null) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.save).setMessage(getString(R.string.update_followed_tag_with_name, tag.name))
                builder.setNegativeButton(R.string.negative_button_name) { _, _ -> onClickedData = null }
                builder.setOnCancelListener { onClickedData = null }
                builder.setPositiveButton(R.string.positive_button_name) { _, _ ->
                    GlobalScope.launch(Dispatchers.Main) {
                        tag.following = Following(clickedData.idWhenClicked, clickedData.countWhenClicked)
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
            R.id.add_following -> {
                AddTagDialog {
                    GlobalScope.launch {
                        val following = createTagAndOrChangeFollowingState(this@FollowingActivity, it.text.toString())
                        withContext(Dispatchers.Main) {
                            layoutManager.scrollToPositionWithOffset(filteringFollowingList.indexOfFirst { it.name == following?.name }, 0)
                        }
                    }
                }.withTitle(getString(R.string.follow_tag)).build(this)
            }
            R.id.update_following -> {
                ConfirmDialog {
                    GlobalScope.launch { updateFollowing(filteringFollowingList) }
                }.withTitle(getString(R.string.update_followed_tags)).build(this@FollowingActivity)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    suspend fun updateFollowing(following: Collection<Tag>) {
        withContext(Dispatchers.IO) {
            val id = db.currentServer.newestID()
            if (id != null) {
                following.map {
                    launch {
                        val tag = db.currentServer.getTag(this@FollowingActivity, it.name)
                        if (tag != null) {
                            val tagInDb = db.getTag(tag.name)
                            tagInDb?.following = Following(id, tag.count)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        db.tags.removeOnUpdateListener(updateFollowingListener)
        adapter.util.close()
    }


}

class FollowingData(val name: String, val idWhenClicked: Int, val countWhenClicked: Int)
