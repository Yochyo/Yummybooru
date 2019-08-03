package de.yochyo.yummybooru.events.listeners

import android.widget.Toast
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.events.events.ChangeTagEvent

class DisplayToastFavoriteTagListener : Listener<ChangeTagEvent>() {
    override fun onEvent(e: ChangeTagEvent) {
        if (e.oldTag.isFavorite != e.newTag.isFavorite)
            if (e.newTag.isFavorite)
                Toast.makeText(e.context, "Favorite [${e.newTag.name}]", Toast.LENGTH_SHORT).show()
            else Toast.makeText(e.context, "Unfavorite [${e.newTag.name}]", Toast.LENGTH_SHORT).show()

    }
}