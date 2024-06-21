package de.yochyo.yummybooru.layout.activities.followingactivity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventcollection.events.OnChangeObjectEvent
import de.yochyo.eventmanager.EventHandler
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Following
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.databinding.ActivityFollowingBinding
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.layout.alertdialogs.ProgressDialog
import de.yochyo.yummybooru.utils.TagUtil
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandUpdateFollowingTagData
import de.yochyo.yummybooru.utils.commands.CommandUpdateSeveralFollowingTagData
import de.yochyo.yummybooru.utils.general.Configuration
import de.yochyo.yummybooru.utils.observeUntil
import de.yochyo.yummybooru.utils.withValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FollowingActivity : AppCompatActivity() {
    lateinit var binding: ActivityFollowingBinding
    var onClickedData: FollowingData? = null
    lateinit var viewModel: FollowingActivityViewModel

    lateinit var recyclerView: RecyclerView
    lateinit var adapter: FollowingTagAdapter
    lateinit var layoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFollowingBinding.inflate(layoutInflater)
        viewModel = ViewModelProvider(this).get(FollowingActivityViewModel::class.java)
        viewModel.init(this)
        Configuration.setWindowSecurityFrag(this, window)
        val oldName = savedInstanceState?.getString("name")
        val oldId = savedInstanceState?.getInt("id")
        val oldCount = savedInstanceState?.getInt("count")
        if (oldName != null && oldId != null && oldCount != null) onClickedData = FollowingData(oldName, oldId, oldCount)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbarFollowing)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        recyclerView = binding.content.followingRecycler
        recyclerView.layoutManager = LinearLayoutManager(this@FollowingActivity).apply { layoutManager = this }

        recyclerView.adapter = FollowingTagAdapter(this, recyclerView).apply { adapter = this }
        binding.followingFilter.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) viewModel.filter.value = newText
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })
        binding.content.followingSwipeRefreshLayout.setOnRefreshListener {
            binding.content.followingSwipeRefreshLayout.isRefreshing = false
            onClickedData = null
            adapter.notifyDataSetChanged()
        }

        registerObservers()
    }

    fun registerObservers() {
        viewModel.tags.observe(this, { adapter.updateFollowing(it) })
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
        adapter.followingObserves.paused = false
        val clickedData = onClickedData ?: return
        onClickedData = null
        viewModel.tags.withValue(this) {
            val tag = it.find { it.name == clickedData.name } ?: return@withValue
            Command.execute(binding.followingLayout, CommandUpdateFollowingTagData(tag, Following(clickedData.idWhenClicked, clickedData.countWhenClicked)))
        }
    }

    override fun onPause() {
        super.onPause()
        adapter.followingObserves.paused = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.add_following -> {
                AddTagDialog { name ->
                    TagUtil.CreateFollowedTagOrChangeFollowing(binding.followingLayout, this@FollowingActivity, name)
                    GlobalScope.launch(Dispatchers.Main) {
                        viewModel.tags.observeUntil(this@FollowingActivity, {
                            val index = it.indexOfFirst { it.name == name }
                            if (index >= 0)
                                layoutManager.scrollToPositionWithOffset(index, 0)
                        }, { it.find { it.name == name } != null })
                    }
                }.withTitle(getString(R.string.follow_tag)).build(this)
            }
            R.id.update_following -> viewModel.tags.withValue(this) { updateFollowing(it) }
        }
        return super.onOptionsItemSelected(item)
    }

    fun updateFollowing(following: Collection<Tag>, message: String = getString(R.string.update_followed_tags)) {
        ConfirmDialog {
            GlobalScope.launch(Dispatchers.IO) {
                val id = viewModel.server.newestID() ?: return@launch

                val observable = EventHandler<OnChangeObjectEvent<Int, Int>>()
                val dialog = withContext(Dispatchers.Main) { ProgressDialog(observable).apply { title = message; build(this@FollowingActivity) } }
                var progress = 0
                val updatedTags = following.map { tag ->
                    async {
                        val pair = Pair(tag, Following(id, viewModel.server.getTag(tag.name).count))
                        observable.trigger(OnChangeObjectEvent(++progress, following.size))
                        pair
                    }
                }.awaitAll()
                Command.execute(binding.followingLayout, CommandUpdateSeveralFollowingTagData(updatedTags))
                withContext(Dispatchers.Main) { dialog.stop() }
            }
        }.withTitle(message).build(this@FollowingActivity)

    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.followingObserves.close()
    }


}

class FollowingData(val name: String, val idWhenClicked: Int, val countWhenClicked: Int)
