package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
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
    var title: String? = null

    fun withServer(server: Server) = apply { this.server = server }
    fun withTitle(s: String) = apply { title = s }

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        val layout = LayoutInflater.from(context).inflate(R.layout.add_server_dialog_view, null) as LinearLayout
        builder.setView(layout)
        val spinner = layout.findViewById<Spinner>(R.id.add_server_api)
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
        adapter.add("auto")
        adapter.addAll(Apis.apis)
        spinner.adapter = adapter
        builder.setTitle(title ?: context.getString(R.string.add_server))

        for (spin in 0 until spinner.adapter.count)
            if ((spinner.adapter.getItem(spin) as String).equals(server.apiName, true)) {
                spinner.setSelection(spin)
                break
            }


        val url = layout.findViewById<TextView>(R.id.add_server_url).apply { text = server.url }
        val username = layout.findViewById<TextView>(R.id.add_server_username).apply { text = server.username }
        val password = layout.findViewById<TextView>(R.id.add_server_password).apply { text = server.password }

        val name = layout.findViewById<TextView>(R.id.add_server_name).apply { text = server.name }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val item = spinner.selectedItem.toString()
                password.hint = when (item) {
                    "auto" -> context.getString(R.string.hint_password_or_api_key)
                    "danbooru" -> context.getString(R.string.hint_api_key)
                    else -> context.getString(R.string.hint_password)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                TODO("Not yet implemented")
            }
        }

        builder.setPositiveButton(context.getString(R.string.positive_button_name)) { _, _ ->
            GlobalScope.launch(Dispatchers.IO) {
                try {
                    val s = Server(name.text.toString(), parseURL(url.text.toString()), spinner.selectedItem.toString(), username.text.toString(),
                            password.text.toString(), id = server.id)
                    if (s.apiName == "Auto") s.apiName = getCorrectApi(context, s)
                    if (s.newestID() == null)
                        withContext(Dispatchers.Main) { Toast.makeText(context, context.getString(R.string.bad_login), Toast.LENGTH_LONG).show() }
                    withContext(Dispatchers.Main) { runOnPositive(s) }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) { Toast.makeText(context, context.getString(R.string.url_not_supported), Toast.LENGTH_LONG).show() }
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