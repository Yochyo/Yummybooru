package de.yochyo.yummybooru.layout.activities.introactivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.general.ctx
import kotlinx.android.synthetic.main.intro_activity_select_save_folder_layout.*

class IntroSelectDirFragment : Fragment(R.layout.intro_activity_select_save_folder_layout) {
    private val SELECT_DIR = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title.text = "Save directory"
        description.text = "Now, select your save directory"
        button.visibility = View.VISIBLE
        button.setOnClickListener {
            startActivityForResult(
                Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                    .apply { putExtra("android.content.extra.SHOW_ADVANCED", true) }, SELECT_DIR
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_DIR) {
            val uri = data?.data
            if (uri != null) {
                ctx.db.saveFolder = DocumentFile.fromTreeUri(ctx, uri)!!
                val activity = activity
                if (activity is IntroActivity) activity.nextSlide()
            }
        }
    }
}