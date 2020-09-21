package de.yochyo.yummybooru.layout.activities.previewactivity

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.booruapi.objects.Post
import de.yochyo.eventcollection.events.OnUpdateEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.DownloadService
import de.yochyo.yummybooru.layout.selectableRecyclerView.ActionModeClickEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.SelectableRecyclerViewAdapter
import de.yochyo.yummybooru.layout.selectableRecyclerView.StartSelectingEvent
import de.yochyo.yummybooru.layout.selectableRecyclerView.StopSelectingEvent
import de.yochyo.yummybooru.utils.ManagerWrapper
import de.yochyo.yummybooru.utils.general.toBooruTag
import de.yochyo.yummybooru.utils.network.downloader
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
                val posts = selected.getSelected(m.posts)
                DownloadService.startService(activity, m.toString(), posts, activity.db.currentServer)
                deselectAll()
            }
            R.id.download_and_add_authors_selected -> {
                val posts = selected.getSelected(m.posts)
                GlobalScope.launch {
                    for (post in posts) {
                        for (tag in post.tags) {
                            if (tag.type == Tag.ARTIST) activity.db.tags += tag.toBooruTag(activity)
                        }
                    }
                }
                DownloadService.startService(activity, m.toString(), posts, activity.db.currentServer)
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

    override fun createViewHolder(parent: ViewGroup) = PreviewViewHolder(activity, m,
            if (activity.db.previewStaggeredMode)
                (activity.layoutInflater.inflate(R.layout.preview_image_view_staggered, parent, false) as FrameLayout)
            else (activity.layoutInflater.inflate(R.layout.preview_image_view, parent, false) as FrameLayout))

    override fun onViewAttachedToWindow(holder: PreviewViewHolder) {
        val pos = holder.adapterPosition
        val p = m.posts[holder.adapterPosition]
        downloader.downloadPostPreviewIMG(activity, p, {
            if (pos == holder.adapterPosition)
                GlobalScope.launch(Dispatchers.Main) { holder.layout.findViewById<ImageView>(R.id.preview_picture).setImageBitmap(it.bitmap) }
        }, activity.isScrolling)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): PreviewViewHolder {
        val holder = super.onCreateViewHolder(parent, position)
        return holder
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.layout.findViewById<ImageView>(R.id.preview_picture).setImageBitmap(null)
    }

    override fun getItemCount(): Int = size
}