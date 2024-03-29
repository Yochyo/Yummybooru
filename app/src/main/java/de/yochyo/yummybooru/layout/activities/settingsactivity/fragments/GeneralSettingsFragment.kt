package de.yochyo.yummybooru.layout.activities.settingsactivity.fragments

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import de.yochyo.yummybooru.R

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "default"
        setPreferencesFromResource(R.xml.settings_general, rootKey)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}