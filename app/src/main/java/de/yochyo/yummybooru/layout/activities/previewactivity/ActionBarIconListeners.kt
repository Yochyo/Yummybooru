package de.yochyo.yummybooru.layout.activities.previewactivity

import android.content.Context
import android.view.Menu
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.events.events.*
import de.yochyo.yummybooru.utils.general.drawable

class ActionBarListener(val context: Context, tag: String, menu: Menu) {
    private var registered: Boolean = false
    fun registerListeners() {
        if (!registered) {
            registered = true
            AddTagEvent.registerListener(addTagListener)
            DeleteTagEvent.registerListener(removeTagListener)
            ChangeTagEvent.registerListener(favoriteTagListener)
            AddSubEvent.registerListener(addSubListener)
            DeleteSubEvent.registerListener(deleteSubListener)
        }
    }

    fun unregisterListeners() {
        if (registered) {
            registered = false
            AddTagEvent.removeListener(addTagListener)
            DeleteTagEvent.removeListener(removeTagListener)
            ChangeTagEvent.removeListener(favoriteTagListener)
            AddSubEvent.removeListener(addSubListener)
            DeleteSubEvent.removeListener(deleteSubListener)
        }
    }

    private val addTagListener = Listener.create<AddTagEvent> {
        if (it.tag.name == tag) {
            menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.remove)
            if (it.tag.isFavorite) menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.favorite)
        }
    }
    private val removeTagListener = Listener.create<DeleteTagEvent> {
        if (it.tag.name == tag) {
            menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.add)
            menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
        }
    }
    private val favoriteTagListener = Listener.create<ChangeTagEvent> {
        if (it.newTag.name == tag) {
            if (it.newTag.isFavorite) menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.favorite)
            else menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
        }
    }
    private val addSubListener = Listener.create<AddSubEvent> {
        if (it.sub.name == tag) {
            menu.findItem(R.id.subscribe).icon = context.drawable(R.drawable.star)
        }
    }
    private val deleteSubListener = Listener.create<DeleteSubEvent> {
        if (it.sub.name == tag) {
            menu.findItem(R.id.subscribe).icon = context.drawable(R.drawable.star_empty)
        }
    }
}