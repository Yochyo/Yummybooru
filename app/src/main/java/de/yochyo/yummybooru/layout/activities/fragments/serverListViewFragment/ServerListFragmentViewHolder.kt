package de.yochyo.yummybooru.layout.activities.fragments.serverListViewFragment

import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog

class ServerListFragmentViewHolder(val fragment: ServerListFragment, val adapter: ServerListFragmentAdapter, val layout: LinearLayout, var server: Server) :
    RecyclerView.ViewHolder(layout), View.OnClickListener, View.OnLongClickListener {

    val viewModel = fragment.viewModel
    val context = layout.context
    override fun onClick(v: View?) {
        context.preferences.currentServerId = server.id
    }

    override fun onLongClick(v: View?): Boolean {
        longClickDialog()
        return true
    }

    private fun longClickDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(server.name)
        builder.setItems(arrayOf(context.getString(R.string.edit_server), context.getString(R.string.delete_server))) { dialog, i ->
            dialog.cancel()
            when (i) {
                0 -> editServerDialog(server)
                1 -> {
                    if (server == viewModel.selectedServerValue.value) Toast.makeText(context, context.getString(R.string.cannot_delete_server), Toast.LENGTH_LONG).show()
                    else ConfirmDialog { context.db.serverDao.delete(server) }.withTitle(context.getString(R.string.delete_server_with_name, server.name)).build(context)
                }
            }
        }
        builder.show()
    }

    private fun editServerDialog(server: Server) {
        AddServerDialog(context) {
            context.db.updateServer(server)
            Toast.makeText(context, context.getString(R.string.edit_server_with_name, it.name), Toast.LENGTH_SHORT).show()
        }.withServer(server).withTitle(context.getString(R.string.edit_server)).build(context)
    }
}
