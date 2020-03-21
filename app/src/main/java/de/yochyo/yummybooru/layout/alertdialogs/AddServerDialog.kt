package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Apis
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.utils.general.parseURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddServerDialog(val runOnPositive: (s: Server) -> Unit) {
    var server = Server("", "", "", "", "")
    var title = "Add server"

    fun withServer(server: Server) = apply { this.server = server }
    fun withTitle(s: String) = apply { title = s }

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.add_server_dialog_view, null) as LinearLayout
        builder.setView(layout)
        val spinner = layout.findViewById<Spinner>(R.id.add_server_api)
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
        adapter.add("Auto")
        adapter.addAll(Apis.apis)
        spinner.adapter = adapter
        builder.setMessage(context.getString(R.string.add_server))

        val name = layout.findViewById<TextView>(R.id.add_server_name).apply { text = server.name }
        val apiSpinner = layout.findViewById<Spinner>(R.id.add_server_api).apply {
            //Falls ein Server übergeben wurde
            for (spin in 0 until this.adapter.count)
                if ((this.adapter.getItem(spin) as String).equals(server.apiName, true)) {
                    this.setSelection(spin)
                    break
                }
        }
        val url = layout.findViewById<TextView>(R.id.add_server_url).apply { text = server.url }
        val username = layout.findViewById<TextView>(R.id.add_server_username).apply { text = server.username }
        val password = layout.findViewById<TextView>(R.id.add_server_password).apply { text = server.password }

        builder.setPositiveButton(context.getString(R.string.ok)) { _, _ ->
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val s = Server(name.text.toString(), parseURL(url.text.toString()), apiSpinner.selectedItem.toString(), username.text.toString(),
                            password.text.toString(), id = server.id)
                    if (s.apiName == "Auto") s.apiName = getCorrectApi(context, s)
                    if (s.newestID() == null)
                        withContext(Dispatchers.Main) { Toast.makeText(context, "(Probably) bad login", Toast.LENGTH_LONG).show() }
                    withContext(Dispatchers.Main) { runOnPositive(s) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Url is wrong or not supported", Toast.LENGTH_LONG).show() }
                }
            }
        }
        builder.show()
    }


    private suspend fun getCorrectApi(context: Context, s: Server): String {
        for (api in Apis.apis) {
            if (s.newestID() != 0) return api
        }
        return Apis.apis.first()
    }
}