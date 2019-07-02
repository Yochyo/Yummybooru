package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.DeleteTagEvent

class DisplayToastDeleteTagListener : Listener<DeleteTagEvent>{
    override fun onEvent(e: DeleteTagEvent): Boolean {
        Toast.makeText(e.context, "Delete tag [${e.tag.name}]", Toast.LENGTH_SHORT).show()
        return true
    }
}