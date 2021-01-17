package de.yochyo.yummybooru.layout.activities.settingsactivity

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SeekBarPreference
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.utils.general.updateCombinedSearchSortAlgorithm

class PreviewActivitySettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "default"
        setPreferencesFromResource(R.xml.settings_preview_activity, rootKey)
        val seekbar = findPreference<SeekBarPreference>(getString(R.string.combined_search_sort))
        seekbar?.setOnPreferenceChangeListener { preference, newValue ->
            updateCombinedSearchSortAlgorithm(newValue.toString().toInt())
            true
        }
    }

}