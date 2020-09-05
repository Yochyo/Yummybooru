package de.yochyo.yummybooru.layout.activities.introactivity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.utils.general.ctx
import kotlinx.android.synthetic.main.intro_activity_select_save_folder_layout.*

class IntroWelcomeFragment : Fragment(R.layout.intro_activity_select_save_folder_layout) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title.text = ctx.getString(R.string.app_name)
        description.text = "Yummybooru is an easy to use booru viewer"
    }
}