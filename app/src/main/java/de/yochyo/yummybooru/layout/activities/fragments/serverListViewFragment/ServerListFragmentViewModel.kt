package de.yochyo.yummybooru.layout.activities.fragments.serverListViewFragment

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.LiveDataValue

class ServerListFragmentViewModel : ViewModel() {
    lateinit var servers: LiveData<List<Server>>
    lateinit var selectedServer: LiveData<Server>
    lateinit var selectedServerValue: LiveDataValue<Server>

    fun init(context: Context) {
        servers = context.db.servers
        selectedServer = context.db.selectedServer
        selectedServerValue = context.db.selectedServerLiveValue
    }
}