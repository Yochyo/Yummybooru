package de.yochyo.yummybooru.utils

import android.content.Context
import android.widget.Toast
import de.yochyo.eventcollection.events.OnAddElementsEvent
import de.yochyo.eventcollection.events.OnChangeObjectEvent
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
            DatabaseListeners.registerListeners(context)
            ToastListeners.registerListeners(context)
        }
    }

    fun unregisterGlobalListeners(context: Context) {
        if (registered) {
            registered = false
            DatabaseListeners.unregisterListeners(context)
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
                    val message = "${if (element.lastId != null && element.lastCount != null) "Following" else if (element.isFavorite) "Add favorite tag" else "Add tag"} " +
                            "[${element.name}]"
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

private object DatabaseListeners {
    //The listeners in this class automatically update the database when the ObservingEventCollection changes
    private lateinit var addServerListener: Listener<OnAddElementsEvent<Server>>
    private lateinit var removeServerListener: Listener<OnRemoveElementsEvent<Server>>
    private lateinit var changeServerListener: Listener<OnChangeObjectEvent<Server, Int>>
    private lateinit var addTagListener: Listener<OnAddElementsEvent<Tag>>
    private lateinit var removeTagListener: Listener<OnRemoveElementsEvent<Tag>>
    private lateinit var changeTagListener: Listener<OnChangeObjectEvent<Tag, Int>>
    fun registerListeners(context: Context) {
        val db = context.db
        addTagListener = Listener { GlobalScope.launch(Dispatchers.IO) { db.tagDao.insert(it.elements) } }
        removeTagListener = Listener { GlobalScope.launch(Dispatchers.IO) { it.elements.forEach { element -> db.tagDao.delete(element) } } }
        changeTagListener = Listener { GlobalScope.launch(Dispatchers.IO) { db.tagDao.update(it.new) } }

        addServerListener = Listener { GlobalScope.launch(Dispatchers.IO) { for (server in it.elements) server.id = db.serverDao.insert(server).toInt() } }
        removeServerListener = Listener { GlobalScope.launch(Dispatchers.IO) { it.elements.forEach { element -> db.serverDao.delete(element) } } }
        changeServerListener = Listener { GlobalScope.launch(Dispatchers.IO) { db.serverDao.update(it.new) } }

        with(db.tags) {
            registerOnAddElementsListener(addTagListener)
            registerOnRemoveElementsListener(removeTagListener)
            registerOnElementChangeListener(changeTagListener)
        }
        with(db.servers) {
            registerOnAddElementsListener(addServerListener)
            registerOnRemoveElementsListener(removeServerListener)
            registerOnElementChangeListener(changeServerListener)
        }

    }

    fun unregisterListeners(context: Context) {
        val db = context.db
        db.servers.removeOnAddElementsListener(addServerListener)
        db.servers.removeOnRemoveElementsListener(removeServerListener)
        db.servers.removeOnElementChangeListener(changeServerListener)

        db.tags.removeOnAddElementsListener(addTagListener)
        db.tags.removeOnRemoveElementsListener(removeTagListener)
        db.tags.removeOnElementChangeListener(changeTagListener)
    }
}