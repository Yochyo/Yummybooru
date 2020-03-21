package de.yochyo.yummybooru.layout.activities.subscriptionactivity

import android.widget.FrameLayout
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableViewHolder
import de.yochyo.yummybooru.utils.general.currentServer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SubscribedTagViewHolder(val activity: SubscriptionActivity, layout: FrameLayout) : SelectableViewHolder(layout) {
    override fun onClickLayout() {
        val tag = activity.filteringSubList.elementAt(adapterPosition)
        GlobalScope.launch {
            val id = activity.currentServer.newestID()
            val count = activity.currentServer.getTag(tag.name)
            if(id != null && count != null)activity.onClickedData = SubData(adapterPosition, id, count.count)
        }
        PreviewActivity.startActivity(activity, "id:>${tag.sub?.lastID ?: Int.MAX_VALUE} ${tag.name}")
    }
}