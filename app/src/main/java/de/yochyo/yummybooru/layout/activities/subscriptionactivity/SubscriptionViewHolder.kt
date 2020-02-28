package de.yochyo.yummybooru.layout.activities.subscriptionactivity

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableViewHolder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SubscribedTagViewHolder(val activity: SubscriptionActivity, layout: FrameLayout) : SelectableViewHolder(layout) {
    override fun onClickLayout() {
        val sub = activity.currentFilter.elementAt(adapterPosition)
        GlobalScope.launch {
            activity.onClickedData = SubData(adapterPosition, Api.newestID(activity), Api.getTag(activity, sub.name).count)
        }
        PreviewActivity.startActivity(activity, sub.toString())
    }
}