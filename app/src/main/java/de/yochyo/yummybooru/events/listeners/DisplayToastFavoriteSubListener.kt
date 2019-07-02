package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.ChangeSubEvent

class DisplayToastFavoriteSubListener : Listener<ChangeSubEvent> {
    override fun onEvent(e: ChangeSubEvent): Boolean {
        if (e.oldSub.isFavorite != e.newSub.isFavorite) {
            if (e.newSub.isFavorite)
                Toast.makeText(e.context, "Favorite [${e.newSub.name}]", Toast.LENGTH_SHORT).show()
            else Toast.makeText(e.context, "Unfavorite [${e.newSub.name}]", Toast.LENGTH_SHORT).show()
            return true
        }
        return false
    }
}