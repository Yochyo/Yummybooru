package de.yochyo.yummybooru.layout.activities.mainactivity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.settingsactivity.SettingsActivity
import de.yochyo.yummybooru.layout.activities.fragments.ServerListViewFragment
import de.yochyo.yummybooru.layout.activities.fragments.TagHistoryFragment
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.activities.followingactivity.FollowingActivity
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.menus.SettingsNavView
import de.yochyo.yummybooru.updater.AutoUpdater
import de.yochyo.yummybooru.updater.Changelog
import de.yochyo.yummybooru.utils.general.cache
import de.yochyo.yummybooru.utils.general.toTagString
import kotlinx.android.synthetic.main.main_activity_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 122)


        setContentView(R.layout.main_activity_layout)
        configureToolbarAndNavView(nav_view)
        if (hasPermission)
            initData()
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.none { it != PackageManager.PERMISSION_GRANTED })
            initData()
    }


    fun initData() {
        supportFragmentManager.beginTransaction().replace(R.id.main_activity_container, ServerListViewFragment()).commit()
        supportFragmentManager.beginTransaction().replace(R.id.main_activity_right_drawer_container, TagHistoryFragment().apply {
            onSearchButtonClick = {
                this@MainActivity.drawer_layout.closeDrawer(GravityCompat.END)
                PreviewActivity.startActivity(this@MainActivity, if (it.isEmpty()) "*" else it.toTagString())
            }
        }).commit()
        Changelog.showChangelogIfChanges(this)
        AutoUpdater().autoUpdate(this)
        GlobalScope.launch { cache.clearCache() }
    }

    private fun configureToolbarAndNavView(navView: NavigationView) {
        setSupportActionBar(main_activity_toolbar)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, main_activity_toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        navView.setNavigationItemSelectedListener(SettingsNavView(navView)
                .apply { inflateMenu(R.menu.activity_main_drawer_menu, this@MainActivity::onNavigationItemSelected) })
    }

    fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_following -> startActivity(Intent(this, FollowingActivity::class.java))
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.community -> startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://discord.gg/tbGCHpF") })
            R.id.nav_help -> Toast.makeText(this, getString(R.string.join_discord), Toast.LENGTH_SHORT).show()
            else -> return false
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_server -> AddServerDialog {
                GlobalScope.launch {
                    db.servers += it
                    withContext(Dispatchers.Main) { Snackbar.make(drawer_layout, "Add server [${it.name}]", Snackbar.LENGTH_SHORT).show() }
                }
            }.build(this)
            R.id.search -> drawer_layout.openDrawer(GravityCompat.END)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
            drawer_layout.isDrawerOpen(GravityCompat.END) -> drawer_layout.closeDrawer(GravityCompat.END)
        }
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch { cache.clearCache() }
    }
}
