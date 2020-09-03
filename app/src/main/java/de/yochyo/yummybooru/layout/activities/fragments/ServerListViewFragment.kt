package de.yochyo.yummybooru.layout.activities.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.utils.general.ctx
import de.yochyo.yummybooru.utils.general.setColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerListViewFragment : Fragment() {
    private lateinit var serverAdapter: ServerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.server_list_fragment, container, false)
        val serverRecyclerView = layout.findViewById<RecyclerView>(R.id.server_recycler_view)
        serverRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        serverAdapter = ServerAdapter().apply { serverRecyclerView.adapter = this }
        GlobalScope.launch {
            inflater.context.db.servers.registerOnUpdateListener { GlobalScope.launch(Dispatchers.Main) { serverAdapter.update(it.collection) } }
            withContext(Dispatchers.Main) { serverAdapter.update(inflater.context.db.servers) }
        }
        return layout
    }

    inner class ServerAdapter : RecyclerView.Adapter<ServerViewHolder>() {
        var servers: Collection<Server> = emptyList()
        override fun getItemCount(): Int = servers.size

        override fun onCreateViewHolder(parent: ViewGroup, position: Int): ServerViewHolder {
            val holder = ServerViewHolder((LayoutInflater.from(context).inflate(R.layout.server_item_layout, parent, false) as LinearLayout),
            servers.elementAt(position))
            holder.layout.setOnClickListener(holder)
            holder.layout.setOnLongClickListener(holder)
            return holder
        }

        override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
            val server = servers.elementAt(position)
            holder.server = server
            fillServerLayoutFields(holder.layout, server, server.isSelected(ctx))
        }

        fun update(s: Collection<Server>){
            servers = s
            notifyDataSetChanged()
        }

        fun fillServerLayoutFields(layout: LinearLayout, server: Server, isSelected: Boolean = false) {
            val text1 = layout.findViewById<TextView>(R.id.server_text1)
            text1.text = server.name
            if (isSelected) text1.setColor(R.color.dark_red)
            else text1.setColor(R.color.violet)
            layout.findViewById<TextView>(R.id.server_text2).text = server.apiName
            layout.findViewById<TextView>(R.id.server_text3).text = server.username
        }

    }

    inner class ServerViewHolder(val layout: LinearLayout, var server: Server) : RecyclerView.ViewHolder(layout), View.OnClickListener, View.OnLongClickListener {

        override fun onClick(v: View?) {
            GlobalScope.launch(Dispatchers.Main) {
                ctx.db.loadServer(server)
                serverAdapter.notifyDataSetChanged()
            }
        }

        override fun onLongClick(v: View?): Boolean {
            longClickDialog(server)
            return true
        }

        private fun longClickDialog(server: Server) {
            val builder = AlertDialog.Builder(ctx)
            builder.setTitle(server.name)
            builder.setItems(arrayOf(ctx.getString(R.string.edit_server), ctx.getString(R.string.delete_server))) { dialog, i ->
                dialog.cancel()
                when (i) {
                    0 -> editServerDialog(server)
                    1 -> {
                        if (!server.isSelected(ctx)) ConfirmDialog { ctx.db.servers -= server }.withTitle(ctx.getString(R.string.delete_server_with_name, server.name)).build(ctx)
                        else Toast.makeText(ctx, ctx.getString(R.string.cannot_delete_server), Toast.LENGTH_LONG).show()
                    }
                }
            }
            builder.show()
        }

        private fun editServerDialog(server: Server) {
            AddServerDialog {
                GlobalScope.launch {
                    server.name = it.name
                    server.url = it.url
                    server.apiName = it.apiName
                    server.username = it.username
                    server.password = it.password
                    withContext(Dispatchers.Main) { Toast.makeText(ctx, getString(R.string.edit_server_with_name, it.name), Toast.LENGTH_SHORT).show() }
                }
            }.withServer(server).withTitle(ctx.getString(R.string.edit_server)).build(ctx)
        }
    }
}