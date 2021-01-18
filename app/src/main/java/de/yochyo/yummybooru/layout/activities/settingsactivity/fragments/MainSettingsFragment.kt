package de.yochyo.yummybooru.layout.activities.settingsactivity.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.backup.BackupUtils
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.alertdialogs.RestoreBackupDailog
import de.yochyo.yummybooru.updater.AutoUpdater
import de.yochyo.yummybooru.updater.Changelog
import de.yochyo.yummybooru.utils.general.FileUtils
import de.yochyo.yummybooru.utils.general.ctx
import de.yochyo.yummybooru.utils.general.logFirebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainSettingsFragment : PreferenceFragmentCompat() {
    private val SAVE_PATH_CODE = 1
    private val RESTORE_DATA_CODE = 2

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "default"
        setPreferencesFromResource(R.xml.settings_main, rootKey)
        setSavePathSummary()
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val res = super.onPreferenceTreeClick(preference)
        when (preference.key) {
            "savePath" ->
                startActivityForResult(
                    Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
                        .apply { putExtra("android.content.extra.SHOW_ADVANCED", true) }, SAVE_PATH_CODE
                )

            "create_backup" ->
                GlobalScope.launch {
                    val context = requireContext()
                    val backupResult = BackupUtils.createBackup(context)
                    withContext(Dispatchers.Main) {
                        if (backupResult)
                            Toast.makeText(context, getString(R.string.new_backup, ctx.db.saveFolder.uri), Toast.LENGTH_LONG).show()
                        else
                            Toast.makeText(context, getString(R.string.backup_failed), Toast.LENGTH_LONG).show()
                    }
                }
            "restore_backup" -> {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.putExtra("android.content.extra.SHOW_ADVANCED", true)
                intent.type = "*/*"
                startActivityForResult(intent, RESTORE_DATA_CODE)
            }
            "updates" -> {
                AutoUpdater(ctx).autoUpdate()
                Toast.makeText(requireContext(), getString(R.string.checking_for_updates), Toast.LENGTH_SHORT).show()
            }
            "changelogs" -> Changelog.showChangelogs(requireContext())
        }
        return res
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == SAVE_PATH_CODE) { //Speicherpfad ändern
                FileUtils.setSaveFolder(ctx, data.data!!)
                setSavePathSummary()
            }
            if (requestCode == RESTORE_DATA_CODE) {
                val stream = requireContext().contentResolver.openInputStream(data.data!!)
                GlobalScope.launch(Dispatchers.IO) {
                    val bytes = stream!!.readBytes()
                    stream.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, getString(R.string.wait_for_backup_restore), Toast.LENGTH_LONG).show()
                        val (flow, size) = BackupUtils.restoreBackup(bytes, requireContext()) ?: Pair(null, 0)
                        if (flow != null) {
                            val dialog = RestoreBackupDailog(size).apply { build(ctx) }
                            flow.collect { dialog.progress++ }
                            dialog.stop()
                        }
                        Toast.makeText(context, getString(R.string.restored_backup), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setSavePathSummary() {
        try {
            val pref = findPreference<Preference>(getString(R.string.savePath))
            if (pref != null) {
                val file = ctx.db.saveFolder
                pref.summary = file.getName()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            e.logFirebase(ctx.db.saveFolderUri).send()
            //TODO this try catch block shouldn't be necessary, but sometimes
        }
    }
}