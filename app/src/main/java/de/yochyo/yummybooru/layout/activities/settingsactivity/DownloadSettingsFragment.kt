package de.yochyo.yummybooru.layout.activities.settingsactivity

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import de.yochyo.yummybooru.R

class DownloadSettingsFragment : PreferenceFragmentCompat(){
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "default"
        setPreferencesFromResource(R.xml.settings_downloads, rootKey)
    }
}