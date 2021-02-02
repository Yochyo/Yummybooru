package de.yochyo.yummybooru.layout.activities.followingactivity

import android.widget.FrameLayout
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableViewHolder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FollowingTagViewHolder(val activity: FollowingActivity, layout: FrameLayout) : SelectableViewHolder(layout) {
    override fun onClickLayout() {
        val tag = activity.filteringFollowingList.elementAt(adapterPosition)
        GlobalScope.launch {
            val id = activity.db.currentServer.newestID()
            val count = activity.db.currentServer.getTag(activity, tag.name)
            if (id != null && count != null) activity.onClickedData = FollowingData(tag.name, id, count.count)
        }
        val string = tag.name.split(" OR ").joinToString(" OR ") { "id:>${tag.following?.lastID ?: Int.MAX_VALUE} $it" }
        PreviewActivity.startActivity(activity, string)
    }
}