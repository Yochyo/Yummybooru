package de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.TagHistoryFragmentViewModel
import de.yochyo.yummybooru.utils.general.ctx

class TagHistoryTagFragment : Fragment() {
    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var tagAdapter: TagHistoryFragmentTagAdapter
    lateinit var tagLayoutManager: LinearLayoutManager

    lateinit var viewModel: TagHistoryFragmentViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        viewModel = ViewModelProvider(requireActivity()).get(TagHistoryFragmentViewModel::class.java)

        val layout = inflater.inflate(R.layout.fragment_tag_history, container, false) as ViewGroup
        tagRecyclerView = RecyclerView(layout.context)
        tagAdapter = TagHistoryFragmentTagAdapter(this).apply { tagRecyclerView.adapter = this }
        tagRecyclerView.layoutManager = LinearLayoutManager(ctx).apply { tagLayoutManager = this }

        registerObservers()

        return layout
    }

    fun registerObservers() {
        viewModel.tags.observe(viewLifecycleOwner, { tagAdapter.update(it) })
        viewModel.selectedTags.observe(viewLifecycleOwner, { tagAdapter.notifyDataSetChanged() })
    }


}