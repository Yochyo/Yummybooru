package de.yochyo.yBooru.layout

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import de.yochyo.yBooru.R
import de.yochyo.yBooru.manager.PreviewManager
import kotlinx.android.synthetic.main.activity_preview.*
import kotlinx.android.synthetic.main.content_preview.*

class PreviewActivity : AppCompatActivity() {
    private lateinit var previewManager: PreviewManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preview)
        setSupportActionBar(toolbar)
        previewManager = PreviewManager(this, recycler_view, intent.getStringExtra("tags").split(" ").toTypedArray())
        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            previewManager.reloadView()
        }
    }
}
