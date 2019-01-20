package de.yochyo.yBooru.layout

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import de.yochyo.yBooru.R
import de.yochyo.yBooru.api.Api
import de.yochyo.yBooru.manager.Manager
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PictureActivity : AppCompatActivity() {
    private val currentTags = ArrayList<PostTag>()
    lateinit var m: Manager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        setSupportActionBar(toolbar_picture)

        m = Manager.get(intent.getStringExtra("tags"))
        nav_view_picture.bringToFront()
        val post = m.currentPost
        if (post != null) {
            currentTags.apply { addAll(post.tagsCopyright.map { PostTag(it, PostTag.COYPRIGHT) });addAll(post.tagsArtist.map { PostTag(it, PostTag.ARTIST) }); addAll(post.tagsCharacter.map { PostTag(it, PostTag.CHARACTER) }); addAll(post.tagsGeneral.map { PostTag(it, PostTag.GENERAL) }) }
            GlobalScope.launch {
                val bitmap = Api.downloadImage(this@PictureActivity, post.fileLargeURL, "${post.id}Large")
                launch(Dispatchers.Main) { image_view.setImageBitmap(bitmap) }
            }
        }
        val recycleView = nav_view_picture.getHeaderView(0).findViewById<RecyclerView>(R.id.recycle_view_info)
        recycleView.adapter = Adapter()
        recycleView.layoutManager = LinearLayoutManager(this)
    }

    private inner class Adapter : RecyclerView.Adapter<InfoButtonHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoButtonHolder = InfoButtonHolder(LayoutInflater.from(parent.context).inflate(R.layout.info_item_button, parent, false) as Button)

        override fun getItemCount(): Int = currentTags.size

        override fun onBindViewHolder(holder: InfoButtonHolder, position: Int) {
            holder.button.text = currentTags[position].name
        }
    }

    private inner class InfoButtonHolder(val button: Button) : RecyclerView.ViewHolder(button)
}

private class PostTag(val name: String, val type: String) {
    companion object {
        val GENERAL = "g"
        val CHARACTER = "c"
        val COYPRIGHT = "cr"
        val ARTIST = "a"
    }

    val color: Int
        get() {
            return 1
        }
}
