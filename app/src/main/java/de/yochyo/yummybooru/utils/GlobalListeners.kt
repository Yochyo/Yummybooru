package de.yochyo.yummybooru.utils

import android.content.Context
import android.widget.Toast
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventcollection.events.OnRemoveElementsEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object GlobalListeners {
    private var registered = false

    fun registerGlobalListeners(context: Context) {
        if (!registered) {
            registered = true
            ToastListeners.registerListeners(context)
        }
    }

    fun unregisterGlobalListeners(context: Context) {
        if (registered) {
            registered = false
            ToastListeners.unregisterListeners(context)
        }
    }
}

private object ToastListeners {
    private lateinit var onAddTags: Listener<OnAddElementsEvent<Tag>>
    private lateinit var onRemoveTags: Listener<OnRemoveElementsEvent<Tag>>

    private lateinit var onAddServers: Listener<OnAddElementsEvent<Server>>
    private lateinit var onRemoveServers: Listener<OnRemoveElementsEvent<Server>>
    fun registerListeners(context: Context) {
        val db = context.db
        onAddTags = Listener {
            GlobalScope.launch(Dispatchers.Main) {
                it.elements.forEach { element ->
                    val message = "${
                        if (element.following != null) "Following" else if (element.isFavorite) "Add favorite tag"
                        else "Add tag"
                    } [${element.name}]"
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
        onRemoveTags = Listener {//On remove tag
            GlobalScope.launch(Dispatchers.Main) { it.elements.forEach { element -> Toast.makeText(context, "\"Delete tag [${element.name}]\"", Toast.LENGTH_SHORT).show() } }
        }

        onAddServers = Listener { //On add server
            GlobalScope.launch(Dispatchers.Main) {
                it.elements.forEach { element -> Toast.makeText(context, "Add server [${element.name}]", Toast.LENGTH_SHORT).show() }
            }
        }
        onRemoveServers = Listener { //On add server
            GlobalScope.launch(Dispatchers.Main) {
                it.elements.forEach { element -> Toast.makeText(context, "Removed server [${element.name}]", Toast.LENGTH_SHORT).show() }
            }
        }

        with(db.tags) {
            registerOnAddElementsListener(onAddTags)
            registerOnRemoveElementsListener(onRemoveTags)
        }
        with(db.servers) {
            registerOnAddElementsListener(onAddServers)
            registerOnRemoveElementsListener(onRemoveServers)
        }
    }

    fun unregisterListeners(context: Context) {
        val db = context.db
        db.tags.removeOnAddElementsListener(onAddTags)
        db.tags.removeOnRemoveElementsListener(onRemoveTags)
        db.servers.removeOnAddElementsListener(onAddServers)
        db.servers.removeOnRemoveElementsListener(onRemoveServers)
    }
}