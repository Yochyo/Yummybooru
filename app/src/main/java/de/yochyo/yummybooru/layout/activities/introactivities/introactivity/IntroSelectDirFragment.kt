package de.yochyo.yummybooru.layout.activities.introactivities.introactivity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.layout.activities.introactivities.IIntroActivity
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.general.ctx

class IntroSelectDirFragment(val text: String = "Save directory", val des: String = "Now, select your save directory") : Fragment(
    R.layout
        .intro_activity_select_save_folder_layout
) {
    private val SELECT_DIR = 1

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.title)
        val description = view.findViewById<TextView>(R.id.description)
        val button = view.findViewById<TextView>(R.id.button)

        title.text = text
        description.text = des
        button.visibility = View.VISIBLE
        button.setOnClickListener { startActivityForResult(FileUtils.getSaveFolderIntent(view.context), SELECT_DIR) }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SELECT_DIR) {
            val uri = data?.data
            if (uri != null) {
                FileUtils.setSaveFolder(ctx, uri)
                val activity = activity
                if (activity is IIntroActivity) activity.moveToNextSlide()
            }
        }
    }
}