package de.yochyo.yummybooru.layout.activities.mainactivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.database.preferences
import de.yochyo.yummybooru.layout.activities.followingactivity.FollowingActivity
import de.yochyo.yummybooru.layout.activities.fragments.serverListViewFragment.ServerListFragment
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview.TagHistoryFragment
import de.yochyo.yummybooru.layout.activities.fragments.tagHistoryFragment.recyclerview_with_tag_collections.TagHistoryCollectionFragment
import de.yochyo.yummybooru.layout.activities.introactivities.introactivity.IntroActivity
import de.yochyo.yummybooru.layout.activities.introactivities.savefolderactivity.SaveFolderChangerActivity
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.activities.settingsactivity.SettingsActivity
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.menus.SettingsNavView
import de.yochyo.yummybooru.updater.AutoUpdater
import de.yochyo.yummybooru.updater.Changelog
import de.yochyo.yummybooru.utils.general.toTagString
import de.yochyo.yummybooru.utils.general.updateCombinedSearchSortAlgorithm
import de.yochyo.yummybooru.utils.general.updateNomediaFile
import kotlinx.android.synthetic.main.main_activity_layout.*

class MainActivity : AppCompatActivity() {
    private var serverListFragment: Fragment? = null
    private var tagHistoryFragment: Fragment? = null

    companion object {
        private const val TAG_FRAGMENT = "tag_fragment"
        private const val SERVER_FRAGMENT = "server_fragment"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)
        updateCombinedSearchSortAlgorithm(preferences.combinedSearchSort)
        if (preferences.isFirstStart)
            startActivity(Intent(this, IntroActivity::class.java))
        else {
            try {
                if (!preferences.saveFolder.exists())
                    startActivity(Intent(this, SaveFolderChangerActivity::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
                startActivity(Intent(this, SaveFolderChangerActivity::class.java))
            }
            try {
                updateNomediaFile(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        configureToolbarAndNavView(nav_view)
        initData(savedInstanceState)
    }

    fun initData(bundle: Bundle?) {
        if (bundle != null) {
            tagHistoryFragment =
                supportFragmentManager.getFragment(bundle, TAG_FRAGMENT) as TagHistoryFragment
            serverListFragment = supportFragmentManager.getFragment(bundle, SERVER_FRAGMENT)
        }
        if (serverListFragment == null) serverListFragment = ServerListFragment()
        //if (tagHistoryFragment == null) tagHistoryFragment = TagHistoryFragment()
        if (tagHistoryFragment == null) tagHistoryFragment = TagHistoryCollectionFragment()

        val frag = tagHistoryFragment
        if (frag is TagHistoryFragment) {
            frag.onSearchButtonClick = {
                this@MainActivity.drawer_layout.closeDrawer(GravityCompat.END)
                PreviewActivity.startActivity(this@MainActivity, if (it.isEmpty()) "*" else it.toTagString())
            }
        }
        if (frag is TagHistoryCollectionFragment) {
            frag.onSearchButtonClick = {
                this@MainActivity.drawer_layout.closeDrawer(GravityCompat.END)
                PreviewActivity.startActivity(this@MainActivity, if (it.isEmpty()) "*" else it.toTagString())
            }
        }


        supportFragmentManager.beginTransaction()
            .replace(R.id.main_activity_container, serverListFragment!!).commit()
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_activity_right_drawer_container, tagHistoryFragment!!).commit()
        Changelog.showChangelogIfChanges(this)
        AutoUpdater(this).autoUpdate()


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
            R.id.action_add_server -> AddServerDialog(this) { db.serverDao.insert(it) }.build(this)
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
