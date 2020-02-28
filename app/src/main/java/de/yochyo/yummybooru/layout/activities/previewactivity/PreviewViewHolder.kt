package de.yochyo.yummybooru.layout.activities.previewactivity

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import de.yochyo.yummybooru.layout.activities.pictureactivity.PictureActivity
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableViewHolder
import de.yochyo.yummybooru.utils.Manager

class PreviewViewHolder(val activity: AppCompatActivity, val m: Manager, layout: FrameLayout) : SelectableViewHolder(layout) {
    override fun onClickLayout() {
        m.position = layoutPosition
        PictureActivity.startActivity(activity, m)
    }
}