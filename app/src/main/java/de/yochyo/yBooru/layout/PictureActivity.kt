package de.yochyo.yBooru.layout

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.yochyo.yBooru.R
import de.yochyo.yBooru.api.Api
import de.yochyo.yBooru.manager.Manager
import kotlinx.android.synthetic.main.activity_picture.*
import kotlinx.android.synthetic.main.content_picture.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PictureActivity : AppCompatActivity() {
    lateinit var m: Manager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture)
        setSupportActionBar(toolbar)

        val tags = intent.getStringExtra("tags")
        println(":::::: $tags")
        m = Manager.get(tags)
        val post = m.dataSet[m.position]
        if (post != null) {
            GlobalScope.launch {
                val bitmap = Api.downloadImage(this@PictureActivity, post.fileLargeURL, "${post.id}Large")
                launch(Dispatchers.Main) { image_view.setImageBitmap(bitmap) }
            }
        }
    }

}
