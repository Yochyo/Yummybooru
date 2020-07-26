package de.yochyo.yummybooru.layout.activities.mainactivity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.android.material.snackbar.Snackbar
import de.yochyo.eventcollection.events.OnRemoveElementsEvent
import de.yochyo.eventmanager.Listener
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.fragments.MainDrawerFragment
import de.yochyo.yummybooru.layout.activities.fragments.ServerListViewFragment
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.updater.AutoUpdater
import de.yochyo.yummybooru.updater.Changelog
import de.yochyo.yummybooru.utils.GlobalListeners
import de.yochyo.yummybooru.utils.general.cache
import kotlinx.android.synthetic.main.main_drawer_fragment.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {
    private lateinit var mainDrawerFragment: MainDrawerFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 122)
        setContentView(R.layout.empty_layout)

        if (hasPermission)
            initData()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.none { it != PackageManager.PERMISSION_GRANTED })
            initData()
    }


    fun initData() {
        GlobalScope.launch { cache.clearCache() }
        mainDrawerFragment = MainDrawerFragment()
        supportFragmentManager.beginTransaction().replace(R.id.container, mainDrawerFragment).commit()
        mainDrawerFragment.withContainer { supportFragmentManager.beginTransaction().replace(it.id, ServerListViewFragment()).commit() }

        Changelog.showChangelogIfChanges(this)
        AutoUpdater().autoUpdate(this)
    }


    override fun onBackPressed() {
        if (!mainDrawerFragment.onBackPress()) super.onBackPressed()
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch { cache.clearCache() }
    }
}
