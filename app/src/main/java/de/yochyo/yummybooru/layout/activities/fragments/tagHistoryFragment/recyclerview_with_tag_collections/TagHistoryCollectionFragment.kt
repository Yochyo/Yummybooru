package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview_with_tag_collections

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
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.entities.TagCollection
import de.yochyo.yummybooru.databinding.FragmentTagHistoryBinding
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.TagHistoryFragmentViewModel
import de.yochyo.yummybooru.layout.alertdialogs.AddSpecialTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.InputDialog
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandAddTag
import de.yochyo.yummybooru.utils.commands.CommandAddTagCollection
import de.yochyo.yummybooru.utils.commands.execute
import de.yochyo.yummybooru.utils.general.ctx
import de.yochyo.yummybooru.utils.observeUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TagHistoryCollectionFragment : Fragment() {
    lateinit var binding: FragmentTagHistoryBinding
    private lateinit var collectionAdapter: TagHistoryCollectionAdapter
    lateinit var tagLayoutManager: LinearLayoutManager

    var onSearchButtonClick: (tags: List<String>) -> Unit = {}
    lateinit var viewModel: TagHistoryFragmentViewModel

    companion object {
        private const val SELECTED = "SELECTED"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(TagHistoryFragmentViewModel::class.java)
        viewModel.init(this, ctx)
        val array = savedInstanceState?.getStringArray(SELECTED)
        if (array != null)
            viewModel.selectedTags.value = array.toList()

        collectionAdapter = TagHistoryCollectionAdapter(this)
        tagLayoutManager = LinearLayoutManager(ctx)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTagHistoryBinding.inflate(inflater, container, false)
        configureTagDrawer(binding.root)

        binding.recyclerViewSearch.adapter = collectionAdapter
        binding.recyclerViewSearch.layoutManager = tagLayoutManager

        registerObservers()
        return binding.root
    }

    fun registerObservers() {
        viewModel.collections.observe(viewLifecycleOwner, { collectionAdapter.update(it.map { it.toExpandableGroup() }) })
        viewModel.selectedTags.observe(viewLifecycleOwner, { collectionAdapter.notifyDataSetChanged() })
    }

    private fun configureTagDrawer(layout: ViewGroup) {
        configureDrawerToolbar(layout.findViewById(R.id.search_toolbar))
        layout.findViewById<SearchView>(R.id.tag_filter).setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) viewModel.filter.value = newText
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })

    }

    private fun configureDrawerToolbar(toolbar: Toolbar) {
        toolbar.inflateMenu(R.menu.tag_collection_fragment_menu)
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
                        GlobalScope.launch(Dispatchers.Main) {
                            val t = viewModel.selectedServer.value?.getTag(it) ?: return@launch
                            if (Command.executeAsync(binding.fragmentTagHistory, CommandAddTag(t))) {
                                viewModel.tags.observeUntil(this@TagHistoryCollectionFragment, {
                                    val index = it.indexOfFirst { it.name == t.name }
                                    if (index >= 0)
                                        tagLayoutManager.scrollToPositionWithOffset(index, 0)
                                }, { it.find { it.name == t.name } != null })
                            }
                        }
                    }.build(ctx)
                }

                R.id.add_special_tag -> viewModel.selectedServer.value?.apply { AddSpecialTagDialog().build(binding.fragmentTagHistory, this) }
                R.id.add_collection -> viewModel.selectedServer.value?.apply {
                    InputDialog {
                        CommandAddTagCollection(
                            TagCollection(
                                it,
                                this.id
                            )
                        ).execute(binding.fragmentTagHistory)
                    }.withTitle("Add tag collection").withHint("Name").build(ctx)
                }

                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }

    }
}
