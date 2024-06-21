package de.yochyo.yummybooru.layout.activities.introactivities.introactivity

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.yochyo.yummybooru.R

class IntroDoneFragment : Fragment(R.layout.intro_activity_select_save_folder_layout) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title = view.findViewById<TextView>(R.id.title)
        val description = view.findViewById<TextView>(R.id.description)
        title.text = "Done"
        description.text = "Enjoy the app"
    }
}