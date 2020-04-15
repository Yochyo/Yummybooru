package de.yochyo.yummybooru.layout.activities.pictureactivity

import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.RecyclerView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.menus.Menus
import de.yochyo.yummybooru.utils.general.createTagAndOrChangeSubState
import de.yochyo.yummybooru.utils.general.setColor
import de.yochyo.yummybooru.utils.general.toBooruTag
import de.yochyo.yummybooru.utils.general.underline
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TagInfoAdapter(val activity: AppCompatActivity) : RecyclerView.Adapter<InfoButtonHolder>() {
    private val db = activity.db

    private var tags: Collection<de.yochyo.booruapi.objects.Tag> = emptyList()

    fun updateInfoTags(t: Collection<de.yochyo.booruapi.objects.Tag>) {
        tags = t
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoButtonHolder = InfoButtonHolder(activity.layoutInflater.inflate(R.layout.info_item_button, parent, false) as Toolbar).apply {
        toolbar.inflateMenu(R.menu.picture_info_menu)
        toolbar.setOnClickListener {
            PreviewActivity.startActivity(activity, toolbar.findViewById<TextView>(R.id.info_textview).text.toString())
            activity.drawer_picture.closeDrawer(GravityCompat.END)
        }
        toolbar.setOnMenuItemClickListener {
            val tag = tags.elementAt(adapterPosition)
            when (it.itemId) {
                R.id.picture_info_item_add_history -> GlobalScope.launch { db.tags += tag.toBooruTag(activity) }
                R.id.picture_info_item_add_favorite -> {
                    GlobalScope.launch {
                        val t = db.getTag(tag.name)
                        if (t == null) db.tags += tag.toBooruTag(activity).apply { isFavorite = true }
                        else t.isFavorite = !t.isFavorite
                        withContext(Dispatchers.Main) { notifyItemChanged(adapterPosition) }
                    }
                }
                R.id.picture_info_item_subscribe -> {
                    GlobalScope.launch {
                        createTagAndOrChangeSubState(activity, tag.name)
                    }
                }
            }
            true
        }
    }

    override fun onBindViewHolder(holder: InfoButtonHolder, position: Int) {
        val tag = tags.elementAt(position).toBooruTag(activity)
        val tagInDatabase = db.getTag(tag.name)
        val textView = holder.toolbar.findViewById<TextView>(R.id.info_textview)
        textView.text = tag.name
        textView.setColor(tag.color)
        textView.underline(tagInDatabase != null && tagInDatabase.isFavorite)
        Menus.initPictureInfoTagMenu(holder.toolbar.menu, tagInDatabase ?: tag)
    }

    override fun getItemCount(): Int = tags.size
}