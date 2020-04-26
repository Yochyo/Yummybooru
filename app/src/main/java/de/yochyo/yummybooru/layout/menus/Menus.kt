package de.yochyo.yummybooru.layout.menus

import android.content.Context
import android.view.Menu
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.utils.general.drawable

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

    fun initPreviewMenu(context: Context, menu: Menu, tag: Tag?) {
        if (tag == null) {
            menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.add)
            menu.findItem(R.id.add_tag).title = "Add to history"
            menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
            menu.findItem(R.id.favorite).title = "Subscribe"
        } else {
            menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.remove)
            menu.findItem(R.id.add_tag).title = "Remove from history"
            if (tag.isFavorite) {
                menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.favorite)
                menu.findItem(R.id.favorite).title = "Unfavorite"
            } else {
                menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
                menu.findItem(R.id.favorite).title = "Favorite"
            }

            if (tag.sub == null) {
                menu.findItem(R.id.subscribe).icon = context.drawable(R.drawable.star_empty)
                menu.findItem(R.id.subscribe).title = "Subscribe"
            } else {
                menu.findItem(R.id.subscribe).icon = context.drawable(R.drawable.star)
                menu.findItem(R.id.subscribe).title = "Unsubscribe"
            }
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
            title = if (tag.sub == null) "Subscribe" else "Unsubscribe"
        }
    }

    fun initSubscriptionMenu(menu: Menu, sub: Tag) {
        with(menu.findItem(R.id.subscription_set_favorite)) {
            title = if (sub.isFavorite) "Unfavorite" else "Favorite"
        }
        with(menu.findItem(R.id.subscription_delete)) {
            title = "Delete"
        }
        with(menu.findItem(R.id.subscription_delete_tag)) {
            title = "Delete tag"
        }
    }
}