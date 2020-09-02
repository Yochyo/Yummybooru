package de.yochyo.yummybooru.utils.analytics

import android.os.Bundle
import de.yochyo.yummybooru.api.entities.Server

class AnalyticsSelectServerEvent(private val server: Server) : IAnalyticsEvent {
    override val name: String
        get() = "select_server"

    override fun createBundle(): Bundle {
        val bundle = Bundle()
        bundle.putString("server_name", server.name)
        bundle.putString("server_api_name", server.apiName)
        bundle.putString("server_url", server.url)
        bundle.putString("logged_in", (server.username != null && server.password != null).toString())
        return bundle
    }
}