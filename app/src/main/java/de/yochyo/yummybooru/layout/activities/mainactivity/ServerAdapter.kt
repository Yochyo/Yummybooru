package de.yochyo.yummybooru.layout.activities.mainactivity

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.utils.general.setColor
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerAdapter(val activity: AppCompatActivity) : RecyclerView.Adapter<ServerViewHolder>() {
    override fun getItemCount(): Int = activity.db.servers.size

    private fun longClickDialog(server: Server) {
        val builder = AlertDialog.Builder(activity)
        builder.setItems(arrayOf(activity.getString(R.string.edit_server), activity.getString(R.string.delete_server))) { dialog, i ->
            dialog.cancel()
            when (i) {
                0 -> editServerDialog(server)
                1 -> {
                    if (!server.isSelected(activity)) ConfirmDialog { activity.db.servers -= server }.withTitle(activity.getString(R.string.delete) + " [${server.name}]").build(activity)
                    else Toast.makeText(activity, activity.getString(R.string.cannot_delete_server), Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.show()
    }

    private fun editServerDialog(server: Server) {
        AddServerDialog {
            GlobalScope.launch {
                server.name = it.name
                server.url =  it.url
                server.apiName = it.apiName
                server.username = it.username
                server.password = it.password
                withContext(Dispatchers.Main) { Snackbar.make(activity.drawer_layout, "Edit [${it.name}]", Snackbar.LENGTH_SHORT).show() }
            }
        }.withServer(server).withTitle(activity.getString(R.string.edit_server)).build(activity)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ServerViewHolder {
        val holder = ServerViewHolder((LayoutInflater.from(activity).inflate(R.layout.server_item_layout, parent, false) as LinearLayout))
        holder.layout.setOnClickListener {
            val server = activity.db.servers.elementAt(holder.adapterPosition)
            GlobalScope.launch(Dispatchers.Main) {
                activity.db.loadServer(server)
                MainActivity.selectedTags.clear()
                notifyDataSetChanged()
            }
        }
        holder.layout.setOnLongClickListener {
            val server = activity.db.servers.elementAt(holder.adapterPosition)
            longClickDialog(server)
            true
        }
        return holder
    }

    override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
        val server = activity.db.servers.elementAt(position)
        fillServerLayoutFields(holder.layout, server, server.isSelected(activity))
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