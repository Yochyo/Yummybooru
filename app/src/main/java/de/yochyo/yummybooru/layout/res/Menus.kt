package de.yochyo.yummybooru.layout.res

import android.view.Menu
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db

object Menus {
    fun initMainSearchTagMenu(menu: Menu, tag: Tag) {
        with(menu.findItem(R.id.main_search_favorite_tag)) {
            if (tag.isFavorite) title = "Unfavorite"
            else title = "Favorite"
        }
        with(menu.findItem(R.id.main_search_subscribe_tag)) {
            if (db.getSubscription(tag.name) == null) title = "Subscribe"
            else title = "Unsubscribe"
        }
        with(menu.findItem(R.id.main_search_delete_tag)) {
            title = "Delete"
        }
    }

    fun initPictureInfoTagMenu(menu: Menu, tag: Tag) {
        with(menu.findItem(R.id.picture_info_item_add_history)) {
            title = "Add to history"
        }
        with(menu.findItem(R.id.picture_info_item_add_favorite)) {
            if (tag.isFavorite) title = "Unfavorite"
            else title = "Favorite"
        }
        with(menu.findItem(R.id.picture_info_item_subscribe)) {
            if (db.getSubscription(tag.name) == null) title = "Subscribe"
            else title = "Unsubscribe"
        }
    }
}