package de.yochyo.yummybooru.events.listeners

import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.DeleteTagEvent
import de.yochyo.yummybooru.layout.activities.mainactivity.MainActivity

class RemoveSelectedTagsInMainactivityListener : Listener<DeleteTagEvent>() {
    override fun onEvent(e: DeleteTagEvent) {
        MainActivity.selectedTags.remove(e.tag.name)
    }
}