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
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.layout.activities.followingactivity.FollowingActivity
import de.yochyo.yummybooru.layout.activities.fragments.ServerListViewFragment
import de.yochyo.yummybooru.layout.activities.fragments.TagHistoryFragment
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.activities.settingsactivity.SettingsActivity
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.menus.SettingsNavView
import de.yochyo.yummybooru.updater.AutoUpdater
import de.yochyo.yummybooru.updater.Changelog
import kotlinx.android.synthetic.main.main_activity_layout.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private var serverListFragment: Fragment? = null
    private var tagHistoryFragment: TagHistoryFragment? = null

    companion object {
        private const val TAG_FRAGMENT = "tag_fragment"
        private const val SERVER_FRAGMENT = "server_fragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 122)


        configureToolbarAndNavView(nav_view)
        if (hasPermission)
            initData(savedInstanceState)
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.none { it != PackageManager.PERMISSION_GRANTED })
            initData(null)
    }


    fun initData(bundle: Bundle?) {
        if (bundle != null) {
            tagHistoryFragment =
                supportFragmentManager.getFragment(bundle, TAG_FRAGMENT) as TagHistoryFragment
            serverListFragment = supportFragmentManager.getFragment(bundle, SERVER_FRAGMENT)
        }
        if (serverListFragment == null) serverListFragment = ServerListViewFragment()
        if (tagHistoryFragment == null) tagHistoryFragment = TagHistoryFragment()

        tagHistoryFragment!!.onSearchButtonClick = {
            this@MainActivity.drawer_layout.closeDrawer(GravityCompat.END)
            PreviewActivity.startActivity(this@MainActivity, if (it.isEmpty()) "*" else it.joinToString(" ") { it.name })
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.main_activity_container, serverListFragment!!).commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_activity_right_drawer_container, tagHistoryFragment!!).commit()
        Changelog.showChangelogIfChanges(this)
        AutoUpdater().autoUpdate(this)
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
            R.id.community -> startActivity(Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(getString(R.string.discord_link))
            })
            R.id.nav_help -> Toast.makeText(this, getString(R.string.join_discord), Toast.LENGTH_SHORT).show()
            else -> return false
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val finalServerListFragment = serverListFragment
        val finalTagHistoryFragment = tagHistoryFragment
        if (finalServerListFragment != null)
            supportFragmentManager.putFragment(outState, SERVER_FRAGMENT, finalServerListFragment)
        if (finalTagHistoryFragment != null)
            supportFragmentManager.putFragment(outState, TAG_FRAGMENT, finalTagHistoryFragment)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_server -> AddServerDialog {
                GlobalScope.launch {
                    db.servers += it
                    withContext(Dispatchers.Main) {
                        Snackbar.make(drawer_layout, getString(R.string.add_server_with_name, it.name), Snackbar.LENGTH_SHORT).show()
                    }
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
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(
                GravityCompat.START
            )
            drawer_layout.isDrawerOpen(GravityCompat.END) -> drawer_layout.closeDrawer(GravityCompat.END)
        }
    }
}
