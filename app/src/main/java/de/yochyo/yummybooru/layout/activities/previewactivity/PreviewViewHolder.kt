package de.yochyo.yummybooru.layout.activities.previewactivity

import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import de.yochyo.yummybooru.api.manager.ManagerWrapper
import de.yochyo.yummybooru.layout.activities.pictureactivity.PictureActivity
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableViewHolder

class PreviewViewHolder(val activity: AppCompatActivity, val m: ManagerWrapper, layout: FrameLayout) : SelectableViewHolder(layout) {
    override fun onClickLayout() {
        m.position = layoutPosition
        PictureActivity.startActivity(activity, m)
    }
}