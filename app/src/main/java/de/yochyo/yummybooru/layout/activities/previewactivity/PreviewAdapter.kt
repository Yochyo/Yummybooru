package de.yochyo.yummybooru.layout.activities.previewactivity

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.Post
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.downloadservice.DownloadService
import de.yochyo.yummybooru.layout.selectableRecyclerView.*
import de.yochyo.yummybooru.utils.general.preview
import de.yochyo.yummybooru.utils.manager.ManagerWrapper
import de.yochyo.yummybooru.utils.manager.OnDownloadPageEvent
import de.yochyo.yummybooru.utils.network.download
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PreviewAdapter(val activity: PreviewActivity, val m: ManagerWrapper) : SelectableRecyclerViewAdapter<PreviewViewHolder>(activity, R.menu.preview_activity_selection_menu) {
    private val loadManagerPageUpdateActionModeListener = Listener.create<OnDownloadPageEvent> {
        GlobalScope.launch(Dispatchers.Main) { actionmode?.title = "${selected.size}/${m.posts.size}" }
    }

    private val startSelectionListener = Listener.create<StartSelectingEvent> {
        m.onDownloadPageEvent.registerListener(loadManagerPageUpdateActionModeListener)
    }
    private val stopSelectionListener = Listener.create<StopSelectingEvent> {
        m.onDownloadPageEvent.removeListener(loadManagerPageUpdateActionModeListener)
    }
    private val clickMenuItemListener = Listener.create<ActionModeClickEvent> {
        when (it.menuItem.itemId) {
            R.id.select_all -> if (selected.size == m.posts.size) unselectAll() else selectAll()
            R.id.download_selected -> {
                val posts = selected.getSelected(m.posts)
                DownloadService.startService(activity, m.toString(), posts, Server.getCurrentServer(activity))
                unselectAll()
            }
            R.id.download_and_add_authors_selected -> {
                val posts = selected.getSelected(m.posts)
                GlobalScope.launch {
                    for (post in posts) {
                        for (tag in post.getTags()) {
                            if (tag.type == Tag.ARTIST) activity.db.addTag(tag)
                        }
                    }
                }
                DownloadService.startService(activity, m.toString(), posts, Server.getCurrentServer(activity))
                unselectAll()
            }
        }
    }

    init {
        onStartSelection.registerListener(startSelectionListener)
        onStopSelection.registerListener(stopSelectionListener)
        onClickMenuItem.registerListener(clickMenuItemListener)
    }

    fun updatePosts(newPage: Collection<Post>) {
        notifyItemRangeInserted(m.posts.size - newPage.size, newPage.size)
    }

    override fun createViewHolder(parent: ViewGroup) = PreviewViewHolder(activity, m, (activity.layoutInflater.inflate(R.layout.preview_image_view, parent, false) as FrameLayout))

    override fun onViewAttachedToWindow(holder: PreviewViewHolder) {
        val pos = holder.adapterPosition
        val p = m.posts[holder.adapterPosition]
        download(activity, p.filePreviewURL, activity.preview(p.id), {
            if (pos == holder.adapterPosition)
                GlobalScope.launch(Dispatchers.Main) { it.loadInto(holder.layout.findViewById<ImageView>(R.id.preview_picture)) }
        }, activity.isScrolling, true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): PreviewViewHolder {
        val holder = super.onCreateViewHolder(parent, position)
        return holder
    }

    override fun onBindViewHolder(holder: PreviewViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)
        holder.layout.findViewById<ImageView>(R.id.preview_picture).setImageBitmap(null)
    }

    override fun getItemCount(): Int = m.posts.size
}