package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.layout.alertdialogs.AddSpecialTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandAddTag
import de.yochyo.yummybooru.utils.general.*
import kotlinx.android.synthetic.main.fragment_tag_history.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TagHistoryFragment : Fragment() {
    var onSearchButtonClick: (tags: List<String>) -> Unit = {}

    lateinit var viewModel: TagHistoryFragmentViewModel

    private lateinit var tagAdapter: TagHistoryFragmentAdapter
    private lateinit var tagLayoutManager: LinearLayoutManager

    companion object {
        private const val SELECTED = "SELECTED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(TagHistoryFragmentViewModel::class.java)
        viewModel.init(this, ctx)
        val array = savedInstanceState?.getStringArray(SELECTED)
        if (array != null)
            viewModel.selectedTags.value = array.toList()

        registerObservers()
    }

    fun registerObservers() {
        viewModel.tags.observe(this, { tagAdapter.update(it) })
        viewModel.selectedTags.observe(this, { tagAdapter.notifyDataSetChanged() })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = inflater.inflate(R.layout.fragment_tag_history, container, false) as ViewGroup
        configureTagDrawer(layout)
        return layout
    }

    private fun configureTagDrawer(layout: ViewGroup) {
        configureDrawerToolbar(layout.findViewById(R.id.search_toolbar))
        val tagRecyclerView = layout.findViewById<RecyclerView>(R.id.recycler_view_search)
        tagRecyclerView.layoutManager = LinearLayoutManager(ctx).apply { tagLayoutManager = this }
        layout.findViewById<SearchView>(R.id.tag_filter).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) viewModel.filter.value = newText
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })

        tagAdapter = TagHistoryFragmentAdapter(this).apply { tagRecyclerView.adapter = this }
    }

    private fun configureDrawerToolbar(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.main_search_nav_menu)
        toolbar.setNavigationIcon(R.drawable.clear)
        toolbar.navigationContentDescription = getString(R.string.deselect_tags)
        toolbar.setNavigationOnClickListener {
            viewModel.selectedTags.value = emptyList()
            Toast.makeText(ctx, getString(R.string.deselected_tags), Toast.LENGTH_SHORT).show()
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search -> onSearchButtonClick(viewModel.selectedTags.value ?: listOf("*"))
                R.id.add_tag -> {
                    AddTagDialog {
                        GlobalScope.launch {
                            val t = viewModel.server.getTag(it.text.toString())

                            Command.execute(fragment_tag_history, CommandAddTag(t))
                            /*
                            withContext(Dispatchers.Main) {
                                tagLayoutManager.scrollToPositionWithOffset(filteringTagList?.indexOfFirst { it.name == t.name }
                                    ?: 0, 0)
                            }
                             */
                            //TODO
                        }
                    }.build(ctx)
                }
                R.id.add_special_tag -> AddSpecialTagDialog().build(fragment_tag_history, viewModel.server)
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(SELECTED, (viewModel.selectedTags.value ?: emptyList()).toTypedArray())
    }
}