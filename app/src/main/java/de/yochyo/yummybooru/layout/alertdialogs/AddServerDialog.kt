package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.utils.network.DownloadUtils
import de.yochyo.yummybooru.utils.network.ResponseCodes
import de.yochyo.yummybooru.utils.parseURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddServerDialog(val runOnPositive: (s: Server) -> Unit) {
    var server = Server("", "", "", "", "", false)
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
        adapter.addAll(Api.apis.map { it.name })
        spinner.adapter = adapter
        builder.setMessage(context.getString(R.string.add_server))

        val name = layout.findViewById<TextView>(R.id.add_server_name).apply { text = server.name }
        val apiSpinner = layout.findViewById<Spinner>(R.id.add_server_api).apply {
            //Falls ein Server übergeben wurde
            for (spin in 0 until this.adapter.count)
                if ((this.adapter.getItem(spin) as String).equals(server.api, true)) {
                    this.setSelection(spin)
                    break
                }
        }
        val r18 = layout.findViewById<CheckBox>(R.id.server_enable_r18_filter).apply { isChecked = server.enableR18Filter }
        val url = layout.findViewById<TextView>(R.id.add_server_url).apply { text = server.url }
        val username = layout.findViewById<TextView>(R.id.add_server_username).apply { text = server.userName }
        val password = layout.findViewById<TextView>(R.id.add_server_password).apply { text = server.password }

        builder.setPositiveButton(context.getString(R.string.ok)) { _, _ ->
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val s = Server(name.text.toString(), apiSpinner.selectedItem.toString(), parseURL(url.text.toString()), username.text.toString(),
                            password.text.toString(), r18.isChecked, server.id)
                    if (s.api == "Auto") s.api = getCorrectApi(s)
                    if (DownloadUtils.getUrlResponseCode(Api.instance!!.urlGetPosts(1, arrayOf("*"), 1)) == ResponseCodes.Unauthorized)
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


    private suspend fun getCorrectApi(s: Server): String {
        for (api in Api.apis) {
            Api.selectApi(api.name, s.url)
            if (Api.newestID() != 0) return api.name
        }
        return Api.apis.first().name
    }
}