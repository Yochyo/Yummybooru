package de.yochyo.yummybooru.layout.activities.settingsactivity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.layout.activities.settingsactivity.fragments.MainSettingsFragment
import de.yochyo.yummybooru.utils.general.Configuration

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        Configuration.setWindowSecurityFrag(this, window)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.layout, MainSettingsFragment())
                .commit()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}

