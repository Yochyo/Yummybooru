package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.utils.network.ResponseCodes
import de.yochyo.yummybooru.utils.parseURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class AddServerDialog(val runOnPositive: (s: Server) -> Unit) {
    var s = Server("", "", "", "", "", false, -1)
    var title = "Add server"

    fun withServer(server: Server) = apply { s = server }
    fun withTitle(s: String) = apply { title = s }

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.add_server_dialog_view, null) as LinearLayout
        builder.setView(layout)
        val spinner = layout.findViewById<Spinner>(R.id.add_server_api)
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
        adapter.addAll(Api.apis.map { it.name })
        spinner.adapter = adapter
        builder.setMessage(context.getString(R.string.add_server))

        val name = layout.findViewById<TextView>(R.id.add_server_name).apply { text = s.name }
        val apiSpinner = layout.findViewById<Spinner>(R.id.add_server_api).apply {
            for (spin in 0 until this.adapter.count)
                if ((this.adapter.getItem(spin) as String).equals(s.api, true)) {
                    this.setSelection(spin)
                    break
                }
        }
        val r18 = layout.findViewById<CheckBox>(R.id.server_enable_r18_filter).apply { isChecked = s.enableR18Filter }
        val url = layout.findViewById<TextView>(R.id.add_server_url).apply { text = s.url }
        val username = layout.findViewById<TextView>(R.id.add_server_username).apply { text = s.userName }
        val password = layout.findViewById<TextView>(R.id.add_server_password).apply { text = s.password }



        builder.setPositiveButton(context.getString(R.string.ok)) { _, _ ->
            val s = Server(name.text.toString(), apiSpinner.selectedItem.toString(), parseURL(url.text.toString()), username.text.toString(),
                    password.text.toString(), id = s.id, enableR18Filter = r18.isChecked)
            runOnPositive(s)
            GlobalScope.launch(Dispatchers.IO) {
                val u = URL(Api.instance!!.urlGetPosts(1, arrayOf("*"), 1))
                val conn = u.openConnection() as HttpURLConnection
                conn.addRequestProperty("User-Agent", "Mozilla/5.00");conn.requestMethod = "GET"
                if (conn.responseCode == ResponseCodes.Unauthorized)
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Probably bad login", Toast.LENGTH_SHORT).show() }
            }
        }
        builder.show()
    }
}