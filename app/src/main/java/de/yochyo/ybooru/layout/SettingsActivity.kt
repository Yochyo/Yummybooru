package de.yochyo.ybooru.layout

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.view.MenuItem
import de.yochyo.ybooru.R
import de.yochyo.ybooru.database.database
import de.yochyo.ybooru.manager.Manager

class SettingsActivity : AppCompatPreferenceActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        fragmentManager.beginTransaction().replace(android.R.id.content, GeneralPreferenceFragment()).commit()
    }

    companion object {
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val database = database
            when (preference.key) {
                "limit" -> {
                    database.limit = value.toString().toInt()
                    Manager.resetAll()
                }
                "sortSubs" -> database.sortSubs = value.toString()
                "sortTags" -> database.sortTags = value.toString()
                //Add here
            }
            true
        }

        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        private fun bindPreferenceSummaryToValue(preference: Preference) {
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager.getDefaultSharedPreferences(preference.context).getString(preference.key, ""))
        }
    }

    class GeneralPreferenceFragment : PreferenceFragment() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            addPreferencesFromResource(R.xml.pref_general)
            setHasOptionsMenu(true)

            bindPreferenceSummaryToValue(findPreference("limit"))
            bindPreferenceSummaryToValue(findPreference("sortSubs"))
            bindPreferenceSummaryToValue(findPreference("sortTags"))
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {
            val id = item.itemId
            if (id == android.R.id.home) {
                startActivity(Intent(activity, SettingsActivity::class.java))
                return true
            }
            return super.onOptionsItemSelected(item)
        }
    }

    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
                || GeneralPreferenceFragment::class.java.name == fragmentName
    }

    override fun onIsMultiPane() = isXLargeTablet(this)
}
