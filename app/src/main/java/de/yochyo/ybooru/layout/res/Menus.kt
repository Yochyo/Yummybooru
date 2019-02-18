package de.yochyo.ybooru.layout.res

import android.content.Context
import android.view.Menu
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Tag
import de.yochyo.ybooru.database

object Menus {
    fun initMainSearchTagMenu(context: Context, menu: Menu, tag: Tag) {
        with(menu.findItem(R.id.main_search_favorite_tag)) {
            if (tag.isFavorite) title = "Unfavorite"
            else title = "Favorite"
        }
        with(menu.findItem(R.id.main_search_subscribe_tag)) {
            if (context.database.getSubscription(tag.name) == null) title = "Subscribe"
            else title = "Unsubscribe"
        }
        with(menu.findItem(R.id.main_search_delete_tag)) {
            title = "Delete"
        }
    }

    fun initPictureInfoTagMenu(context: Context, menu: Menu, tag: Tag) {
        with(menu.findItem(R.id.picture_info_item_add_history)) {
            title = "Add to history"
        }
        with(menu.findItem(R.id.picture_info_item_add_favorite)) {
            if (tag.isFavorite) title = "Unfavorite"
            else title = "Favorite"
        }
        with(menu.findItem(R.id.picture_info_item_subscribe)) {
            if (context.database.getSubscription(tag.name) == null) title = "Subscribe"
            else title = "Unsubscribe"
        }
    }
}