package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.eventcollection.events.OnRemoveElementsEvent
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.database.eventcollections.TagEventCollection
import de.yochyo.yummybooru.events.events.SelectServerEvent
import de.yochyo.yummybooru.layout.alertdialogs.AddSpecialTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandAddTag
import de.yochyo.yummybooru.utils.general.*
import kotlinx.android.synthetic.main.fragment_tag_history.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagHistoryFragment : Fragment() {
    var onSearchButtonClick: (tags: List<String>) -> Unit = {}
    private var filteringTagList: FilteringEventCollection<Tag, Int>? = null


    private lateinit var tagAdapter: TagHistoryFragmentAdapter
    private lateinit var tagLayoutManager: LinearLayoutManager


    private val selectedTags = ArrayList<String>()

    private val selectedTagRemovedListener = Listener<OnRemoveElementsEvent<Tag>> { selectedTags.removeAll(it.elements.map { it.name }) }
    private val selectedServerChangedEvent = Listener<SelectServerEvent> {
        if (it.oldServer != it.newServer)
            selectedTags.clear()
    }
    private val onUpdateTags = Listener<OnUpdateEvent<Tag>> { GlobalScope.launch(Dispatchers.Main) { tagAdapter.update(filteringTagList!!) } }

    companion object {
        private const val SELECTED = "SELECTED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val array = savedInstanceState?.getStringArray(SELECTED)
        if (array != null)
            selectedTags += array
        GlobalScope.launch {
            ctx.db.tags.registerOnRemoveElementsListener(selectedTagRemovedListener)
            SelectServerEvent.registerListener(selectedServerChangedEvent)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_tag_history, container, false) as ViewGroup
        configureTagDrawer(layout)
        filteringTagList = FilteringEventCollection({ inflater.context.db.tags }, { it.name }, { TagEventCollection.getInstance(ctx) })
        inflater.context.db.tags.registerOnUpdateListener(onUpdateTags)
        tagAdapter.update(inflater.context.db.tags)
        return layout
    }

    private fun configureTagDrawer(layout: ViewGroup) {
        configureDrawerToolbar(layout.findViewById(R.id.search_toolbar))
        val tagRecyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view_search)
        tagRecyclerView.layoutManager = LinearLayoutManager(ctx).apply { tagLayoutManager = this }
        layout.findViewById<SearchView>(R.id.tag_filter).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) GlobalScope.launch { filter(newText) }
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })

        tagAdapter = TagHistoryFragmentAdapter(this, selectedTags).apply { tagRecyclerView.adapter = this }
    }

    private fun configureDrawerToolbar(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.main_search_nav_menu)
        toolbar.setNavigationIcon(R.drawable.clear)
        toolbar.navigationContentDescription = getString(R.string.deselect_tags)
        toolbar.setNavigationOnClickListener {
            selectedTags.clear()
            Toast.makeText(ctx, getString(R.string.deselected_tags), Toast.LENGTH_SHORT).show()
            tagAdapter.notifyDataSetChanged()
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search -> onSearchButtonClick(selectedTags)
                R.id.add_tag -> {
                    AddTagDialog {
                        GlobalScope.launch {
                            val t = ctx.db.currentServer.getTag(ctx, it.text.toString())
                            Command.execute(fragment_tag_history, CommandAddTag(t))
                            withContext(Dispatchers.Main) {
                                tagLayoutManager.scrollToPositionWithOffset(filteringTagList?.indexOfFirst { it.name == t.name }
                                    ?: 0, 0)
                            }
                        }
                    }.build(ctx)
                }
                R.id.add_special_tag -> AddSpecialTagDialog().build(ctx)
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(SELECTED, selectedTags.toTypedArray())
    }

    suspend fun filter(name: String) {
        val result = filteringTagList?.filter(name)
        if (result != null) {
            withContext(Dispatchers.Main) {
                tagLayoutManager.scrollToPosition(0)
                tagAdapter.update(result)
            }
        }
    }

    override fun onDestroy() {
        ctx.db.tags.removeOnRemoveElementsListener(selectedTagRemovedListener)
        ctx.db.tags.removeOnUpdateListener(onUpdateTags)
        SelectServerEvent.removeListener(selectedServerChangedEvent)
        super.onDestroy()
    }

}