package de.yochyo.yummybooru.layout.activities.subscriptionactivity

import android.widget.FrameLayout
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableViewHolder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SubscribedTagViewHolder(val activity: SubscriptionActivity, layout: FrameLayout) : SelectableViewHolder(layout) {
    override fun onClickLayout() {
        val tag = activity.filteringSubList.elementAt(adapterPosition)
        GlobalScope.launch {
            val id = activity.db.currentServer.newestID()
            val count = activity.db.currentServer.getTag(activity, tag.name)
            if (id != null && count != null) activity.onClickedData = SubData(tag.name, id, count.count)
        }
        PreviewActivity.startActivity(activity, "id:>${tag.sub?.lastID ?: Int.MAX_VALUE} ${tag.name}")
    }
}