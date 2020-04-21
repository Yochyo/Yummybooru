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
            ToastListeners.registerListeners(context)
            DatabaseListeners.registerListeners(context)
        }
    }

    fun unregisterGlobalListeners(context: Context) {
        if (registered) {
            registered = false
            ToastListeners.unregisterListeners(context)
            DatabaseListeners.unregisterListeners(context)
        }
    }
}

private object ToastListeners {
    private lateinit var onAddTags: Listener<OnAddElementsEvent<Tag>>
    private lateinit var onRemoveTags: Listener<OnRemoveElementsEvent<Tag>>
    private lateinit var onChangeTag: Listener<OnChangeObjectEvent<Tag, Int>>

    private lateinit var onAddServers: Listener<OnAddElementsEvent<Server>>
    private lateinit var onRemoveServers: Listener<OnRemoveElementsEvent<Server>>
    fun registerListeners(context: Context) {
        val db = context.db
        onAddTags = db.tags.registerOnAddElementsListener { //On add (favorite) tag
            GlobalScope.launch(Dispatchers.Main) { it.elements.forEach { element -> Toast.makeText(context, "Add ${if (element.sub != null) "sub" else if (element.isFavorite) "favorite tag" else "tag"} [${element.name}]", Toast.LENGTH_SHORT).show() } }
        }
        onRemoveTags = db.tags.registerOnRemoveElementsListener {//On remove tag
            GlobalScope.launch(Dispatchers.Main) { it.elements.forEach { element -> Toast.makeText(context, "\"Delete tag [${element.name}]\"", Toast.LENGTH_SHORT).show() } }
        }

        onChangeTag = db.tags.registerOnElementChangeListener { //On change tag
            GlobalScope.launch(Dispatchers.Main) {
                when (it.arg) {
                    Tag.CHANGED_FAVORITE -> {
                        Toast.makeText(context,
                                "${if (it.new.isFavorite) "Favorite" else "Unfavorite"} tag [${it.new.name}]", Toast.LENGTH_SHORT).show()
                    }
                    Tag.CHANGED_TYPE -> Toast.makeText(context,
                            "Changed tag [${it.new.name}]", Toast.LENGTH_SHORT).show()
                    Tag.CHANGED_SUB -> Toast.makeText(context,
                            "${if (it.new.sub == null) "Deleted" else "Changed"} sub [${it.new.name}]", Toast.LENGTH_SHORT).show()
                    Tag.ADD_SUB -> Toast.makeText(context,
                            "Add sub [${it.new.name}]", Toast.LENGTH_SHORT).show()
                }
            }
        }
        onAddServers = db.servers.registerOnAddElementsListener { //On add server
            GlobalScope.launch(Dispatchers.Main) {
                it.elements.forEach { element ->
                    Toast.makeText(context, "Add server [${element.name}]", Toast.LENGTH_SHORT).show()
                }
            }
        }
        onRemoveServers = db.servers.registerOnRemoveElementsListener { //On add server
            GlobalScope.launch(Dispatchers.Main) {
                it.elements.forEach { element ->
                    Toast.makeText(context, "Remove server [${element.name}]", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun unregisterListeners(context: Context) {
        val db = context.db
        db.tags.removeOnAddElementsListener(onAddTags)
        db.tags.removeOnRemoveElementsListener(onRemoveTags)
        db.tags.removeOnElementChangeListener(onChangeTag)
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
        addServerListener = db.servers.registerOnAddElementsListener {
            GlobalScope.launch(Dispatchers.IO) {
                for (server in it.elements)
                    server.id = db.serverDao.insert(server)
            }
        }
        removeServerListener = db.servers.registerOnRemoveElementsListener { GlobalScope.launch(Dispatchers.IO) { it.elements.forEach { element -> db.serverDao.delete(element) } } }
        changeServerListener = db.servers.registerOnElementChangeListener { GlobalScope.launch(Dispatchers.IO) { db.serverDao.update(it.new) } }


        addTagListener = db.tags.registerOnAddElementsListener {
            GlobalScope.launch(Dispatchers.IO) {
                it.elements.forEach { element ->
                    db.tagDao.insert(element)
                }
            }
        }
        removeTagListener = db.tags.registerOnRemoveElementsListener { GlobalScope.launch(Dispatchers.IO) { it.elements.forEach { element -> db.tagDao.delete(element) } } }

        changeTagListener = db.tags.registerOnElementChangeListener { GlobalScope.launch(Dispatchers.IO) { db.tagDao.update(it.new) } }

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