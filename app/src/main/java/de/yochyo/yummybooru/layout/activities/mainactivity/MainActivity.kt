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


    companion object {
        private const val SELECTED = "SELECTED"
        val selectedTags = ArrayList<String>()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //onRestoreActivity
        val array = savedInstanceState?.getStringArray(SELECTED)
        if (array != null)
            selectedTags += array

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArray(SELECTED, selectedTags.toTypedArray())
    }

    fun initData() {
        GlobalScope.launch { cache.clearCache() }
        val mainDrawer = MainDrawerFragment()
        supportFragmentManager.beginTransaction().replace(R.id.container, mainDrawer).commit()
        mainDrawer.withContainer { supportFragmentManager.beginTransaction().replace(it.id, ServerListViewFragment()).commit() }

        initListeners()
        Changelog.showChangelogIfChanges(this)
        AutoUpdater().autoUpdate(this)
    }

    private fun initListeners() {
        val listener = Listener.create<OnRemoveElementsEvent<Tag>> { selectedTags.removeAll(it.elements.map { it.name }) }
        GlobalListeners.addGlobalListener(db.tags.onRemoveElements,
                listener)
        //Global Listeners for whole app
    }


    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
            drawer_layout.isDrawerOpen(GravityCompat.END) -> drawer_layout.closeDrawer(GravityCompat.END)
            else -> super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch { cache.clearCache() }
    }
}
