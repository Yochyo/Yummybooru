package de.yochyo.yummybooru.layout.activities.fragments.serverListViewFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.ctx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ServerListFragment : Fragment() {
    private lateinit var serverAdapter: ServerListFragmentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.server_list_fragment, container, false)
        val serverRecyclerView = layout.findViewById<RecyclerView>(R.id.server_recycler_view)
        serverRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        serverAdapter = ServerListFragmentAdapter(ctx).apply { serverRecyclerView.adapter = this }

        inflater.context.db.servers.registerOnUpdateListener { GlobalScope.launch(Dispatchers.Main) { serverAdapter.update(it.collection) } }
        serverAdapter.update(inflater.context.db.servers)
        return layout
    }

}