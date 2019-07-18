package de.yochyo.yummybooru.layout.res

import android.view.Menu
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db

object Menus {
    fun initMainSearchTagMenu(menu: Menu, tag: Tag) {
        with(menu.findItem(R.id.main_search_favorite_tag)) {
            title = if (tag.isFavorite) "Unfavorite" else "Favorite"
        }
        with(menu.findItem(R.id.main_search_subscribe_tag)) {
            title = if (db.getSubscription(tag.name) == null) "Subscribe" else "Unsubscribe"
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
            title = if (tag.isFavorite) "Unfavorite" else "Favorite"
        }
        with(menu.findItem(R.id.picture_info_item_subscribe)) {
            title = if (db.getSubscription(tag.name) == null) "Subscribe" else "Unsubscribe"
        }
    }
}