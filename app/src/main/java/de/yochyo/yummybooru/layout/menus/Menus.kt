package de.yochyo.yummybooru.layout.menus

import android.content.Context
import android.view.Menu
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.utils.general.drawable

object Menus {
    fun initMainSearchTagMenu(context: Context, menu: Menu, tag: Tag) {
        with(menu.findItem(R.id.main_search_favorite_tag)) {
            title = if (tag.isFavorite) context.getString(R.string.unfavorite)
            else context.getString(R.string.favorite)
        }
        with(menu.findItem(R.id.main_search_follow_tag)) {
            title = if (tag.lastId == null || tag.lastCount == null) context.getString(R.string.follow)
            else context.getString(R.string.unfollow)
        }
        with(menu.findItem(R.id.main_search_delete_tag)) {
            title = context.getString(R.string.delete)
        }
    }

    fun initPreviewMenu(context: Context, menu: Menu, tag: Tag?) {
        if (tag == null) {
            menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.add)
            menu.findItem(R.id.add_tag).title = context.getString(R.string.add_to_history)
            menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
            menu.findItem(R.id.favorite).title = context.getString(R.string.follow)
        } else {
            menu.findItem(R.id.add_tag).icon = context.drawable(R.drawable.remove)
            menu.findItem(R.id.add_tag).title = context.getString(R.string.remove_from_history)
            if (tag.isFavorite) {
                menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.favorite)
                menu.findItem(R.id.favorite).title = context.getString(R.string.unfavorite)
            } else {
                menu.findItem(R.id.favorite).icon = context.drawable(R.drawable.unfavorite)
                menu.findItem(R.id.favorite).title = context.getString(R.string.favorite)
            }

            if (tag.lastId == null || tag.lastCount == null) {
                menu.findItem(R.id.follow).icon = context.drawable(R.drawable.star_empty)
                menu.findItem(R.id.follow).title = context.getString(R.string.follow)
            } else {
                menu.findItem(R.id.follow).icon = context.drawable(R.drawable.star)
                menu.findItem(R.id.follow).title = context.getString(R.string.unfollow)
            }
        }
    }

    fun initPictureInfoTagMenu(context: Context, menu: Menu, tag: Tag) {
        with(menu.findItem(R.id.picture_info_item_add_history)) {
            title = context.getString(R.string.add_to_history)
        }
        with(menu.findItem(R.id.picture_info_item_add_favorite)) {
            title = if (tag.isFavorite) context.getString(R.string.unfavorite) else context.getString(R.string.favorite)
        }
        with(menu.findItem(R.id.picture_info_item_following)) {
            title = if (tag.lastId == null || tag.lastCount == null) context.getString(R.string.follow) else context.getString(R.string.unfavorite)
        }
    }

    fun initFollowingMenu(context: Context, menu: Menu, following: Tag) {
        with(menu.findItem(R.id.following_set_favorite)) {
            title = if (following.isFavorite) context.getString(R.string.unfavorite) else context.getString(R.string.favorite)
        }
        with(menu.findItem(R.id.unfollow)) {
            title = context.getString(R.string.unfollow)
        }
        with(menu.findItem(R.id.delete_tag)) {
            title = context.getString(R.string.delete_tag)
        }
    }
}