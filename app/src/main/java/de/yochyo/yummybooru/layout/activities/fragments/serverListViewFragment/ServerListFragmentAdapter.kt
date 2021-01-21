package de.yochyo.yummybooru.layout.activities.fragments.serverListViewFragment

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.utils.general.setColor

class ServerListFragmentAdapter(val context: Context) : RecyclerView.Adapter<ServerListFragmentViewHolder>() {
    var servers: Collection<Server> = emptyList()
    override fun getItemCount(): Int = servers.size

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ServerListFragmentViewHolder {
        val holder = ServerListFragmentViewHolder(
            this,
            (LayoutInflater.from(context).inflate(R.layout.server_item_layout, parent, false) as LinearLayout),
            servers.elementAt(position)
        )
        holder.layout.setOnClickListener(holder)
        holder.layout.setOnLongClickListener(holder)
        return holder
    }

    override fun onBindViewHolder(holder: ServerListFragmentViewHolder, position: Int) {
        val server = servers.elementAt(position)
        holder.server = server
        fillServerLayoutFields(holder.layout, server, server.isSelected(context))
    }

    fun update(s: Collection<Server>) {
        servers = s
        notifyDataSetChanged()
    }

    fun fillServerLayoutFields(layout: LinearLayout, server: Server, isSelected: Boolean = false) {
        val text1 = layout.findViewById<TextView>(R.id.server_text1)
        text1.text = server.name
        text1.setColor(if (isSelected) R.color.dark_red else R.color.violet)
        layout.findViewById<TextView>(R.id.server_text2).text = server.apiName

        layout.findViewById<ImageView>(R.id.server_logged_in).visibility =
            if (server.username != "" && server.password != "") View.VISIBLE
            else View.INVISIBLE
    }

}