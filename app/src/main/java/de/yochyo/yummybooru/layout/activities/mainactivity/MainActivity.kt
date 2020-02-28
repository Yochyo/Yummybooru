package de.yochyo.yummybooru.layout.activities.mainactivity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.eventcollection.EventCollection
import de.yochyo.eventcollection.SubEventCollection
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.UpdateServersEvent
import de.yochyo.yummybooru.events.events.UpdateTagsEvent
import de.yochyo.yummybooru.layout.activities.SettingsActivity
import de.yochyo.yummybooru.layout.activities.previewactivity.PreviewActivity
import de.yochyo.yummybooru.layout.activities.subscriptionactivity.SubscriptionActivity
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddSpecialTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.updater.AutoUpdater
import de.yochyo.yummybooru.updater.Changelog
import de.yochyo.yummybooru.utils.general.cache
import de.yochyo.yummybooru.utils.general.toTagString
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val currentFilter: EventCollection<Tag> get() = filteredTags.last().first
    private val filteredTags = ArrayList<Pair<EventCollection<Tag>, String>>()
        get() {
            if (field.isEmpty()) field += Pair(db.tags, "")
            return field
        }

    private val filterMutex = Mutex()
    suspend fun filter(name: String) {
        withContext(Dispatchers.Default) {
            filterMutex.withLock {
                var result: EventCollection<Tag>? = null
                if (name == "") { //No Filter
                    for (item in filteredTags) {
                        val list = item.first
                        if (list is SubEventCollection) list.destroy()
                    }
                    filteredTags.clear()
                    result = db.tags
                } else {
                    for (i in filteredTags.indices.reversed()) {
                        if (name.startsWith(filteredTags[i].second)) {
                            result = SubEventCollection(TreeSet(), filteredTags[i].first) { it.name.contains(name) }
                            break
                        }
                    }
                }
                if (result == null) result = SubEventCollection(TreeSet(), db.tags) { it.name.contains(name) }
                withContext(Dispatchers.Main) {
                    tagLayoutManager.scrollToPosition(0)
                    filteredTags += Pair(result, name)
                    tagAdapter.update(result)
                }
            }
        }
    }

    companion object {
        val selectedTags = ArrayList<String>()
    }

    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var tagAdapter: TagAdapter
    private lateinit var tagLayoutManager: LinearLayoutManager

    private lateinit var serverAdapter: ServerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 122)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)


        val navLayout = nav_search.findViewById<LinearLayout>(R.id.nav_search_layout)
        initDrawerToolbar(navLayout.findViewById(R.id.search_toolbar))
        tagRecyclerView = navLayout.findViewById(R.id.recycler_view_search)
        tagRecyclerView.layoutManager = LinearLayoutManager(this).apply { tagLayoutManager = this }
        tag_filter.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) GlobalScope.launch { filter(newText) }
                return true
            }

            override fun onQueryTextSubmit(query: String?) = true
        })
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
        UpdateTagsEvent.registerListener { tagAdapter.update(currentFilter) }
        UpdateServersEvent.registerListener { serverAdapter.notifyDataSetChanged() }

        tagAdapter = TagAdapter(this, currentFilter).apply { tagRecyclerView.adapter = this }
        val serverRecyclerView = findViewById<RecyclerView>(R.id.server_recycler_view)
        serverRecyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        serverAdapter = ServerAdapter(this).apply { serverRecyclerView.adapter = this }

        Changelog.showChangelogIfChanges(this)
        AutoUpdater().autoUpdate(this)

    }

    private fun initDrawerToolbar(toolbar: androidx.appcompat.widget.Toolbar) {
        toolbar.inflateMenu(R.menu.main_search_nav_menu)
        toolbar.setNavigationIcon(R.drawable.clear)
        toolbar.navigationContentDescription = "Unselect all tags"
        toolbar.setNavigationOnClickListener {
            selectedTags.clear()
            Toast.makeText(this, "Unselected all tags", Toast.LENGTH_SHORT).show()
            tagAdapter.notifyDataSetChanged()
        }

        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.search -> {
                    drawer_layout.closeDrawer(GravityCompat.END)
                    PreviewActivity.startActivity(this, if (selectedTags.isEmpty()) "*" else selectedTags.toTagString())
                }
                R.id.add_tag -> {
                    AddTagDialog {
                        GlobalScope.launch {
                            val t = db.addTag(Api.getTag(this@MainActivity, it.text.toString()))
                            withContext(Dispatchers.Main) {
                                tagLayoutManager.scrollToPositionWithOffset(db.tags.indexOf(t), 0)
                            }
                        }
                    }.build(this)
                }
                R.id.add_special_tag -> AddSpecialTagDialog().build(this)
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }

    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_server -> AddServerDialog {
                GlobalScope.launch {
                    db.addServer(it)
                    withContext(Dispatchers.Main) { Snackbar.make(drawer_layout, "Add server [${it.name}]", Snackbar.LENGTH_SHORT).show() }
                }
            }.build(this)
            R.id.search -> drawer_layout.openDrawer(GravityCompat.END)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_subs -> startActivity(Intent(this, SubscriptionActivity::class.java))
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.community -> startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://discord.gg/tbGCHpF") })
            R.id.nav_help -> Toast.makeText(this, getString(R.string.join_discord), Toast.LENGTH_SHORT).show()
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        when {
            drawer_layout.isDrawerOpen(GravityCompat.START) -> drawer_layout.closeDrawer(GravityCompat.START)
            drawer_layout.isDrawerOpen(GravityCompat.END) -> drawer_layout.closeDrawer(GravityCompat.END)
            else -> super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()
        GlobalScope.launch { cache.clearCache() }
    }
}
