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
        with(menu.findItem(R.id.main_search_follow_tag)) {
            title = if (tag.following == null) "Follow" else "Unfollow"
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
            menu.findItem(R.id.favorite).title = "Follow"
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

            if (tag.following == null) {
                menu.findItem(R.id.follow).icon = context.drawable(R.drawable.star_empty)
                menu.findItem(R.id.follow).title = "Follow"
            } else {
                menu.findItem(R.id.follow).icon = context.drawable(R.drawable.star)
                menu.findItem(R.id.follow).title = "Unfollow"
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
        with(menu.findItem(R.id.picture_info_item_following)) {
            title = if (tag.following == null) "Follow" else "Unfollow"
        }
    }

    fun initFollowingMenu(menu: Menu, following: Tag) {
        with(menu.findItem(R.id.following_set_favorite)) {
            title = if (following.isFavorite) "Unfavorite" else "Favorite"
        }
        with(menu.findItem(R.id.unfollow)) {
            title = "Delete"
        }
        with(menu.findItem(R.id.delete_tag)) {
            title = "Delete tag"
        }
    }
}