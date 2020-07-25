package de.yochyo.yummybooru.layout.activities

import android.app.Activity
import android.content.Intent
import de.yochyo.yummybooru.R
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.yochyo.yummybooru.backup.BackupUtils
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.updater.AutoUpdater
import de.yochyo.yummybooru.updater.Changelog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.layout, MySettingsFragment())
                .commit()
    }


}

class MySettingsFragment : PreferenceFragmentCompat() {
    private val SAVE_PATH_CODE = 1
    private val RESTORE_DATA_CODE = 2

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val res = super.onPreferenceTreeClick(preference)
        when (preference.key) {
            "savePath" ->
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        .apply { putExtra("android.content.extra.SHOW_ADVANCED", true) }, SAVE_PATH_CODE)
            "create_backup" ->
                GlobalScope.launch {
                    val context = requireContext()
                    BackupUtils.createBackup(context)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "New backup in [${BackupUtils.directory}]", Toast.LENGTH_LONG).show()
                    }
                }
            "restore_backup" -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
                intent.type = "*/*"
                startActivityForResult(intent, RESTORE_DATA_CODE)
            }
            "updates" -> {
                AutoUpdater().autoUpdate(requireContext())
                Toast.makeText(requireContext(), "Checking for updates", Toast.LENGTH_SHORT).show()
            }
            "changelogs" -> Changelog.showChangelogs(requireContext())
        }
        return res
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == SAVE_PATH_CODE) { //Speicherpfad ändern
                val file = DocumentFile.fromTreeUri(requireContext(), data.data!!)
                requireContext().db.saveFolder = file!!
                setSavePathSummary()
            }
            if (requestCode == RESTORE_DATA_CODE) { //Daten wiederherstellen
                val stream = requireContext().contentResolver.openInputStream(data.data!!)
                GlobalScope.launch(Dispatchers.IO) {
                    val bytes = stream!!.readBytes()
                    stream.close()
                    withContext(Dispatchers.Main) { Toast.makeText(context, "Please wait until the backup is restored", Toast.LENGTH_LONG).show() }
                    BackupUtils.restoreBackup(bytes, requireContext())
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Restored backup", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setSavePathSummary() {
        val pref = findPreference<Preference>("savePath")
        if (pref != null) {
            val file = requireContext().db.saveFolder
            pref.summary = file.name
        }
    }
}