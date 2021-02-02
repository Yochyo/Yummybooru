package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.api.entities.IResource
import de.yochyo.yummybooru.utils.general.mimeType

class PictureAdapter(val activity: PictureActivity) : RecyclerView.Adapter<PictureViewHolder>() {
    private val m = activity.m

    private var size = m.posts.size

    fun updatePosts() {
        if (size < m.posts.size) {
            val pos = size
            val inserted = m.posts.size - size
            size = m.posts.size

            notifyItemRangeInserted(pos, inserted)
        }
    }

    override fun getItemCount() = m.posts.size

    override fun onBindViewHolder(holder: PictureViewHolder, position: Int) {
        holder.layout.tag = position
        val post = m.posts.elementAt(position)
        when (IResource.typeFromMimeType(post.fileSampleURL.mimeType ?: "")) {
            IResource.VIDEO -> holder.loadVideo(post)
            else -> holder.loadImage(post)
        }
        if (position == activity.m.position) holder.resume()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PictureViewHolder {
        val viewHolder = PictureViewHolder(activity)
        return viewHolder
    }
}