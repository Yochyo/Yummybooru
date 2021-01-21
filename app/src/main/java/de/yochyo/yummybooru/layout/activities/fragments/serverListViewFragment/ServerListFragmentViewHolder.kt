package de.yochyo.yummybooru.layout.activities.fragments.serverListViewFragment

import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerListFragmentViewHolder(val adapter: ServerListFragmentAdapter, val layout: LinearLayout, var server: Server) : RecyclerView.ViewHolder(layout), View.OnClickListener,
    View.OnLongClickListener {

    val ctx = layout.context
    override fun onClick(v: View?) {
        GlobalScope.launch(Dispatchers.Main) {
            ctx.db.loadServer(server)
            adapter.notifyDataSetChanged()
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
        AddServerDialog(ctx) {
            GlobalScope.launch {
                server.name = it.name
                server.url = it.url
                server.apiName = it.apiName
                server.username = it.username
                server.password = it.password
                withContext(Dispatchers.Main) { Toast.makeText(ctx, ctx.getString(R.string.edit_server_with_name, it.name), Toast.LENGTH_SHORT).show() }
            }
        }.withServer(server).withTitle(ctx.getString(R.string.edit_server)).build(ctx)
    }
}
