package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.utils.TagUtil
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandAddTag
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.toBooruTag
import de.yochyo.yummybooru.utils.general.underline
import de.yochyo.yummybooru.utils.withValue

class TagInfoAdapter(val activity: PictureActivity) : RecyclerView.Adapter<InfoButtonHolder>() {
    private var tags: List<de.yochyo.booruapi.api.Tag> = emptyList()

    fun updateInfoTags(t: List<de.yochyo.booruapi.api.Tag>) {
        tags = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoButtonHolder =
        InfoButtonHolder(activity.layoutInflater.inflate(R.layout.info_item_button, parent, false) as Toolbar).apply {
            toolbar.inflateMenu(R.menu.picture_info_menu)
            toolbar.setOnClickListener {
                PreviewActivity.startActivity(activity, toolbar.findViewById<TextView>(R.id.info_textview).text.toString())
                activity.binding.content.drawerPicture2.closeDrawer(GravityCompat.END)
            }

            toolbar.setOnMenuItemClickListener {
                val tag = tags[adapterPosition]
                when (it.itemId) {
                    R.id.picture_info_item_add_history -> Command.execute(activity.binding.pictureActivityContainer, CommandAddTag(tag.toBooruTag(activity.viewModel.server)))
                    R.id.picture_info_item_add_favorite -> TagUtil.favoriteOrCreateTagIfNotExist(activity.binding.pictureActivityContainer, activity, tag.name)
                    R.id.picture_info_item_following -> TagUtil.CreateFollowedTagOrChangeFollowing(activity.binding.pictureActivityContainer, activity, tag.name)
                }

                true
            }
        }

    override fun onBindViewHolder(holder: InfoButtonHolder, position: Int) {
        val tag = tags[position].toBooruTag(activity.viewModel.server)
        val tagInDatabase = activity.viewModel.tags.withValue(activity) {
            val tagInDb = it.find { it.name == tag.name }
            val textView = holder.toolbar.findViewById<TextView>(R.id.info_textview)
            textView.text = tag.name
            textView.setColor(tag.color)
            textView.underline(tagInDb != null && tagInDb.isFavorite)
            Menus.initPictureInfoTagMenu(activity, holder.toolbar.menu, tagInDb ?: tag)

        }


    }

    override fun getItemCount(): Int = tags.size
}