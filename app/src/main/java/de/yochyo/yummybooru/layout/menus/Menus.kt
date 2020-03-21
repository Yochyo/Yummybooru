package de.yochyo.yummybooru.layout.menus

import android.content.Context
import android.view.Menu
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag

object Menus {
    fun initMainSearchTagMenu(context: Context, menu: Menu, tag: Tag) {
        with(menu.findItem(R.id.main_search_favorite_tag)) {
            title = if (tag.isFavorite) "Unfavorite" else "Favorite"
        }
        with(menu.findItem(R.id.main_search_subscribe_tag)) {
            title = if (tag.sub == null) "Subscribe" else "Unsubscribe"
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
            title = if (tag.isFavorite) "Unfavorite" else "Favorite"
        }
        with(menu.findItem(R.id.picture_info_item_subscribe)) {
            title = if (tag.name == null) "Subscribe" else "Unsubscribe"
        }
    }

    fun initSubscriptionMenu(menu: Menu, sub: Tag) {
        with(menu.findItem(R.id.subscription_set_favorite)) {
            title = if (sub.isFavorite) "Unfavorite" else "Favorite"
        }
        with(menu.findItem(R.id.subscription_delete)) {
            title = "Delete"
        }
    }
}