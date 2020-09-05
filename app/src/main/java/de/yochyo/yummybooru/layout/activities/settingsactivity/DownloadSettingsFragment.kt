package de.yochyo.yummybooru.layout.activities.settingsactivity

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.utils.general.ctx
import de.yochyo.yummybooru.utils.general.updateNomediaFile

class DownloadSettingsFragment : PreferenceFragmentCompat(){
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "default"
        setPreferencesFromResource(R.xml.settings_downloads, rootKey)

        findPreference<SwitchPreference>(getString(R.string.use_nomedia))?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                updateNomediaFile(ctx, newValue = newValue as Boolean)
                true
            }
    }
}