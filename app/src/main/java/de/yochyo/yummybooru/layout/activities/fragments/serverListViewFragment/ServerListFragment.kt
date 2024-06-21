package de.yochyo.yummybooru.layout.activities.fragments.serverListViewFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.databinding.ServerListFragmentBinding
import de.yochyo.yummybooru.utils.general.ctx

class ServerListFragment : Fragment() {
    lateinit var binding: ServerListFragmentBinding
    lateinit var viewModel: ServerListFragmentViewModel
    private lateinit var serverAdapter: ServerListFragmentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = ServerListFragmentBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ServerListFragmentViewModel::class.java)
        viewModel.init(inflater.context)

        binding.serverRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        serverAdapter = ServerListFragmentAdapter(this).apply { binding.serverRecyclerView.adapter = this }

        registerObservers()
        return binding.root
    }

    fun registerObservers() {
        viewModel.servers.observe(viewLifecycleOwner) { serverAdapter.update(it) }
        viewModel.selectedServer.observe(viewLifecycleOwner) { serverAdapter.notifyDataSetChanged() }
        viewModel.selectedServer.observe(viewLifecycleOwner, { Toast.makeText(ctx, ctx.getString(R.string.selected_server_with_name, it.name), Toast.LENGTH_SHORT).show() })
    }
}