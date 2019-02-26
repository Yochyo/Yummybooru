package de.yochyo.ybooru.layout

import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SubscriptionPreviewActivity : PreviewActivity() {
    companion object {
        fun startActivity(context: Context, tags: String) = context.startActivity(Intent(context, SubscriptionPreviewActivity::class.java).apply { putExtra("tags", tags) })
    }

    override fun loadPage(page: Int) {
        isLoadingView = true
        GlobalScope.launch {
            val i = m.dataSet.size
            val posts = m.getPage(this@SubscriptionPreviewActivity, page)
            launch(Dispatchers.Main) {
                previewAdapter.notifyItemRangeInserted(if (i > 0) i - 1 else 0, posts.size)
                isLoadingView = false
            }
            launch {
                m.downloadPage(this@SubscriptionPreviewActivity, m.currentPage + 1)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        layoutManager.scrollToPosition(m.position)
    }
}
