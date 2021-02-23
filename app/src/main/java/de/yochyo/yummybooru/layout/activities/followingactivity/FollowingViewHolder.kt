package de.yochyo.yummybooru.layout.activities.followingactivity

import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableViewHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FollowingTagViewHolder(val activity: FollowingActivity, layout: FrameLayout) : SelectableViewHolder(layout) {
    var observer: FollowingObservers.FollowingObserver? = null
    override fun onClickLayout() {
        val tag = activity.filteringFollowingList.elementAt(adapterPosition)
        GlobalScope.launch {
            val id = activity.db.currentServer.newestID()
            if (id != null) {
                val count = activity.db.currentServer.getTag(activity, tag.name).count
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
            val pos = adapterPosition
            if (adapterPosition !in activity.filteringFollowingList.indices) return@setListener
            val tag = activity.filteringFollowingList.elementAt(pos)
            GlobalScope.launch(Dispatchers.Main) {
                val toolbar = layout.findViewById<Toolbar>(R.id.toolbar)
                val text2 = toolbar.findViewById<TextView>(android.R.id.text2)
                if (tag == observer.tag)
                    text2.text = activity.getString(R.string.number_of_new_pictures, it.arg)
            }
        }
        observer.start()

        GlobalScope.launch(Dispatchers.IO) { observer.updateCountDifference() }
    }
}