package de.yochyo.yummybooru.layout.activities.followingactivity

import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.max

class FollowingTagViewHolder(val activity: FollowingActivity, layout: FrameLayout, var tag: Tag) : SelectableViewHolder(layout) {
    var observer: FollowingObservers.FollowingObserver? = null
    override fun onClickLayout() {
        GlobalScope.launch {
            val id = activity.viewModel.server.newestID()
            if (id != null) {
                val count = activity.viewModel.server.getTag(tag.name).count
                activity.onClickedData = FollowingData(tag.name, id, count)
            }
        }
        val lastId = "id:>${tag.following?.lastID ?: Int.MAX_VALUE}"
        val string = "$lastId ${tag.name.replace(" THEN ", " THEN $lastId ").replace(" OR ", " OR $lastId ")}"

        PreviewActivity.startActivity(activity, string)
    }

    fun registerObserver(observer: FollowingObservers.FollowingObserver) {
        this.observer?.close()
        this.observer = observer
        observer.setListener {
            GlobalScope.launch(Dispatchers.Main) {
                val toolbar = layout.findViewById<Toolbar>(R.id.toolbar)
                val text2 = toolbar.findViewById<TextView>(android.R.id.text2)
                if (tag == observer.tag) {
                    val difference = max(0, it.new - (tag.following?.lastCount ?: 0))
                    text2.text = activity.getString(R.string.number_of_new_pictures, difference)
                }
            }
        }
        observer.start()

        GlobalScope.launch(Dispatchers.IO) { observer.updateCountDifference() }
    }
}