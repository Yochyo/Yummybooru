package de.yochyo.yummybooru.events.listeners

import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.DeleteTagEvent
import de.yochyo.yummybooru.layout.MainActivity

class RemoveSelectedTagsInMainactivityListener : Listener<DeleteTagEvent> {
    override fun onEvent(e: DeleteTagEvent): Boolean {
        return MainActivity.selectedTags.remove(e.tag.name)
    }
}