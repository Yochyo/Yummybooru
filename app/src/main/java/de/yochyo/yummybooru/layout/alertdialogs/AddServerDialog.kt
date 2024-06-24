package de.yochyo.yummybooru.layout.alertdialogs

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Apis
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.utils.general.Configuration
import de.yochyo.yummybooru.utils.general.parseURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddServerDialog(context: Context, val runOnPositive: (s: Server) -> Unit) {
    var server = Server("", "", "")
    var title: String? = null

    fun withServer(server: Server) = apply { this.server = server }
    fun withTitle(s: String) = apply { title = s }


    private var layout: LinearLayout? = null
    val serverName: String get() = layout!!.findViewById<TextView>(R.id.add_server_name).text.toString()
    val url: String get() = layout!!.findViewById<TextView>(R.id.add_server_url).text.toString()
    val username: String get() = layout!!.findViewById<TextView>(R.id.add_server_username).text.toString()
    val password: String get() = layout!!.findViewById<TextView>(R.id.add_server_password).text.toString()
    val apiName: String get() = layout!!.findViewById<Spinner>(R.id.add_server_api).selectedItem.toString()
    private fun fillLayoutAndCreate(context: Context, server: Server): LinearLayout {
        val layout = if (layout == null) {
            val layout = LayoutInflater.from(context).inflate(R.layout.add_server_dialog_view, null) as LinearLayout
            this.layout = layout

            val spinner = layout.findViewById<Spinner>(R.id.add_server_api)
            val adapter = ArrayAdapter<String>(context, android.R.layout.simple_list_item_1)
            adapter.add("auto")
            adapter.addAll(Apis.apis)
            spinner.adapter = adapter
            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val item = spinner.selectedItem.toString()
                    layout.findViewById<TextView>(R.id.add_server_password).hint = when (item) {
                        "auto" -> context.getString(R.string.hint_password_or_api_key)
                        Apis.DANBOORU -> context.getString(R.string.hint_api_key)
                        else -> context.getString(R.string.hint_password)
                    }
                    layout.findViewById<TextView>(R.id.add_server_username).hint = when (item) {
                        "auto" -> context.getString(R.string.hint_username_or_email)
                        else -> context.getString(R.string.hint_username)
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            val templateSpinner = layout.findViewById<Spinner>(R.id.server_template)
            val templateSpinnerAdapter = ArrayAdapter<Server>(context, android.R.layout.simple_list_item_1)
            templateSpinnerAdapter.add(Server("Template", "", ""))
            templateSpinnerAdapter.addAll(ServerTemplates.templates)
            templateSpinner.adapter = templateSpinnerAdapter
            templateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedServer = templateSpinner.selectedItem as Server
                    if (selectedServer.name != "Template")
                        fillLayoutAndCreate(context, selectedServer)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

            layout
        } else this.layout!!

        val spinner = layout.findViewById<Spinner>(R.id.add_server_api)
        for (spin in 0 until spinner.adapter.count)
            if ((spinner.adapter.getItem(spin) as String).equals(server.apiName, true)) {
                spinner.setSelection(spin)
                break
            }

        layout.findViewById<TextView>(R.id.add_server_name).text = server.name
        layout.findViewById<TextView>(R.id.add_server_url).text = server.url
        layout.findViewById<TextView>(R.id.add_server_username).text = server.username
        layout.findViewById<TextView>(R.id.add_server_password).text = server.password

        return layout
    }

    fun build(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title ?: context.getString(R.string.add_server))
        builder.setView(fillLayoutAndCreate(context, server))

        builder.setPositiveButton(context.getString(R.string.positive_button_name)) { _, _ ->
            GlobalScope.launch(Dispatchers.IO) {
                var s = Server(serverName, parseURL(url), apiName, username, password, id = server.id)
                if (s.apiName == "auto") s = s.copy(apiName = getCorrectApi(s))
                try {
                    if (s.newestID() == null)
                        throw Exception("")
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) { Toast.makeText(context, context.getString(R.string.bad_login), Toast.LENGTH_LONG).show() }
                }

                withContext(Dispatchers.Main) { runOnPositive(s) }
            }
        }
        val dialog = builder.create()
        dialog.window.apply { if (this != null) Configuration.setWindowSecurityFrag(context, this) }
        dialog.show()
    }


    private suspend fun getCorrectApi(s: Server): String {
        for (api in Apis.apis) {
            try {
                if (s.newestID() != null) return api
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Apis.apis.first()
    }
}

private object ServerTemplates {
    val templates = ArrayList<Server>()

    init {
        templates += Server("Danbooru", "https://danbooru.donmai.us/", Apis.DANBOORU)
        templates += Server("Konachan", "https://konachan.com/", Apis.MOEBOORU)
        templates += Server("Yande.re", "https://yande.re/", Apis.MOEBOORU)
        templates += Server("Gelbooru", "https://gelbooru.com/", Apis.GELBOORU)
        templates += Server("Lolibooru", "https://lolibooru.moe/", Apis.MY_IMOUTO)
        templates += Server("rule34.xxx", "https://rule34.xxx/", Apis.GELBOORU_BETA)
        templates += Server("Realbooru", "https://realbooru.com/", Apis.GELBOORU_BETA)
        templates += Server("Furrybooru", "https://safebooru.org/", Apis.GELBOORU_BETA)
    }
}