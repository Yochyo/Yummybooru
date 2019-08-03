package de.yochyo.yummybooru.layout

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import android.support.v4.provider.DocumentFile
import android.view.MenuItem
import android.widget.Toast
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.downloads.Manager
import de.yochyo.yummybooru.backup.BackupUtils
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.utils.documentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsActivity : AppCompatPreferenceActivity() {
    private val savePathCode = 2143421
    private val restoreDataCode = 12344
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        PreferenceManager.setDefaultValues(baseContext, R.xml.pref_general, false)
        addPreferencesFromResource(R.xml.pref_general)
        setSavePathSummary()
        findPreference("savePath").setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
            startActivityForResult(intent, savePathCode)
            true
        }
        findPreference("create_backup").setOnPreferenceClickListener {
            BackupUtils.createBackup(this)
            Toast.makeText(this, "New backup in [${BackupUtils.directory}]", Toast.LENGTH_LONG).show()
            true
        }
        findPreference("restore_backup").setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
            intent.type = "*/*"
            startActivityForResult(intent, restoreDataCode)
            true
        }

        bindPreferenceSummaryToValue(findPreference("limit"))
        bindPreferenceSummaryToValue(findPreference("sortSubs"))
        bindPreferenceSummaryToValue(findPreference("sortTags"))
        bindPreferenceSummaryToValue(findPreference("downloadOriginal"))
        bindPreferenceSummaryToValue(findPreference("savePath"))
    }

    companion object {
        private val sBindPreferenceSummaryToValueListener = Preference.OnPreferenceChangeListener { preference, value ->
            val database = db
            when (preference.key) {
                "limit" -> database.limit = value.toString().toInt()
                "sortSubs" -> database.sortSubs = value.toString()
                "sortTags" -> database.sortTags = value.toString()
                "downloadOriginal" -> database.downloadOriginal = value.toString().toBoolean()
                //Add here
            }
            true
        }

        private fun isXLargeTablet(context: Context): Boolean {
            return context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_XLARGE
        }

        private fun bindPreferenceSummaryToValue(preference: Preference) {
            preference.onPreferenceChangeListener = sBindPreferenceSummaryToValueListener
            if (preference.key == "downloadOriginal")
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.context).getBoolean(preference.key, true))
            else
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, PreferenceManager.getDefaultSharedPreferences(preference.context).getString(preference.key, ""))
        }
    }

    override fun isValidFragment(fragmentName: String): Boolean {
        return PreferenceFragment::class.java.name == fragmentName
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onIsMultiPane() = isXLargeTablet(this)
    //Set savePath
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == savePathCode) { //Speicherpfad ändern
                val file = DocumentFile.fromTreeUri(this, data.data)
                db.savePath = file!!.uri.toString()
                setSavePathSummary()
            }
            if (requestCode == restoreDataCode) { //Daten wiederherstellen
                val stream = contentResolver.openInputStream(data.data)
                GlobalScope.launch(Dispatchers.IO) {
                    val bytes = stream.readBytes()
                    stream.close()
                    BackupUtils.restoreBackup(bytes, this@SettingsActivity)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@SettingsActivity, "Restored backup", Toast.LENGTH_LONG).show()
                        db.initServer(this@SettingsActivity)
                    }
                }
            }
        }
    }

    private fun setSavePathSummary() {
        val pref = findPreference("savePath")
        val file = documentFile(this, db.savePath)
        pref.summary = file!!.name
    }
}
