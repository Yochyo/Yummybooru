package de.yochyo.yummybooru.layout.activities.introactivities.introactivity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import de.yochyo.yummybooru.R
import kotlinx.android.synthetic.main.intro_activity_select_save_folder_layout.*

class IntroPermissionFragment : Fragment(R.layout.intro_activity_select_save_folder_layout) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        title.text = "Permission"
        description.text = "Please give us the permission to store files on your device"
    }
}