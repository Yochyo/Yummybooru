package de.yochyo.ybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.api.Api
import de.yochyo.ybooru.database.entities.Server
import de.yochyo.ybooru.utils.parseURL

class AddServerDialog(val runOnPositive: (s: Server) -> Unit) {
    var serverID = -1
    var nameText = ""
    var apiText = ""
    var urlText = ""
    var userText = ""
    var message = "Add Server"
    var passwordText = ""

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.add_server_dialog_view, null) as LinearLayout
        builder.setView(layout)
        val spinner = layout.findViewById<Spinner>(R.id.add_server_api)
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
        adapter.addAll(Api.apis.map { it.name })
        spinner.adapter = adapter
        builder.setMessage("Change Server")

        val name = layout.findViewById<TextView>(R.id.add_server_name).apply { text = nameText }
        val api = layout.findViewById<Spinner>(R.id.add_server_api).apply {
            for (s in 0 until this.adapter.count)
                if ((this.adapter.getItem(s) as String).equals(apiText, true)) {
                    this.setSelection(s)
                    break
                }
        }
        val url = layout.findViewById<TextView>(R.id.add_server_url).apply { text = urlText }
        val username = layout.findViewById<TextView>(R.id.add_server_username).apply { text = userText }
        val password = layout.findViewById<TextView>(R.id.add_server_password).apply { text = passwordText }



        builder.setPositiveButton("OK") { _, _ ->
            val s = Server(name.text.toString(), api.selectedItem.toString(), parseURL(url.text.toString()), username.text.toString(),
                    password.text.toString(), id = serverID)
            runOnPositive(s)
        }
        builder.create().show()
    }
}