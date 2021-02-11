package de.yochyo.yummybooru.layout.activities.previewactivity

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.booruapi.api.Post
import de.yochyo.booruapi.api.TagType
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.manager.ManagerWrapper
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.DownloadService
import de.yochyo.yummybooru.layout.selectableRecyclerView.ActionModeClickEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableRecyclerViewAdapter
import de.yochyo.yummybooru.layout.selectableRecyclerView.StartSelectingEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.StopSelectingEvent
import de.yochyo.yummybooru.utils.commands.Command
import de.yochyo.yummybooru.utils.commands.CommandAddTag
import de.yochyo.yummybooru.utils.general.toBooruTag
import de.yochyo.yummybooru.utils.network.downloader
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PreviewAdapter(val activity: PreviewActivity, recyclerView: RecyclerView, val m: ManagerWrapper) : SelectableRecyclerViewAdapter<PreviewViewHolder>(
    activity, recyclerView,
    R.menu
        .preview_activity_selection_menu
) {
    private val loadManagerPageUpdateActionModeListener = Listener<OnUpdateEvent<Post>> {
        GlobalScope.launch(Dispatchers.Main) { actionmode?.title = "${selected.size}/${m.posts.size}" }
    }

    private val startSelectionListener = Listener<StartSelectingEvent> {
        m.posts.registerOnUpdateListener(loadManagerPageUpdateActionModeListener)
    }
    private val stopSelectionListener = Listener<StopSelectingEvent> {
        m.posts.removeOnUpdateListener(loadManagerPageUpdateActionModeListener)
    }

    private var size = m.posts.size

    private val clickMenuItemListener = Listener<ActionModeClickEvent> {
        when (it.menuItem.itemId) {
            R.id.select_all -> if (selected.size == m.posts.size) deselectAll() else selectAll()
            R.id.download_selected -> {
                GlobalScope.launch {
                    val posts = selected.getSelected(m.posts)
                    DownloadService.startService(activity, m.toString(), posts, activity.db.currentServer)
                }
                deselectAll()
            }
            R.id.download_and_add_authors_selected -> {
                val posts = selected.getSelected(m.posts)
                GlobalScope.launch {
                    posts.map { post -> post.getTags() }.flatMap { tags -> tags.filter { tag -> tag.tagType == TagType.ARTIST } }.forEach { t ->
                        Command.execute(activity.preview_activity_container, CommandAddTag(t.toBooruTag(activity)))
                    }
                    DownloadService.startService(activity, m.toString(), posts, activity.db.currentServer)
                }
                deselectAll()
            }
        }
    }

    init {
        onStartSelection.registerListener(startSelectionListener)
        onStopSelection.registerListener(stopSelectionListener)
        onClickMenuItem.registerListener(clickMenuItemListener)
    }

    fun updatePosts(newPage: Collection<Post>) {
        size = m.posts.size
        notifyItemRangeInserted(m.posts.size - newPage.size, newPage.size)
    }

    override fun createViewHolder(parent: ViewGroup): PreviewViewHolder {
        val framelayout = if (activity.db.previewStaggeredMode)
            (activity.layoutInflater.inflate(R.layout.preview_image_view_staggered, parent, false) as FrameLayout)
        else getNonStaggeredPreviewView(parent)

        if (activity.db.cropPreviewImage)
            framelayout.findViewById<ImageView>(R.id.preview_picture).scaleType = ImageView.ScaleType.CENTER_CROP
        return PreviewViewHolder(activity, m, framelayout)
    }

    private fun getNonStaggeredPreviewView(parent: ViewGroup): FrameLayout {
        val frameLayout = (activity.layoutInflater.inflate(R.layout.preview_image_view, parent, false) as FrameLayout)
        frameLayout.minimumHeight = frameLayout.width
        return frameLayout
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        if (position !in m.posts.indices) return

        super.onBindViewHolder(holder, position)
        holder.layout.findViewById<ImageView>(R.id.preview_picture).setImageBitmap(null)
        val pos = holder.adapterPosition
        if (pos in m.posts.indices) {
            val p = m.posts[holder.adapterPosition]
            downloader.downloadPostPreviewIMG(activity, p, {
                if (pos == holder.adapterPosition && it != null)
                    GlobalScope.launch(Dispatchers.Main) { holder.layout.findViewById<ImageView>(R.id.preview_picture).setImageBitmap(it.bitmap) }
            }, activity.db.currentServer.headers, activity.isScrolling)
        }
    }

    override fun getItemCount(): Int = size
}