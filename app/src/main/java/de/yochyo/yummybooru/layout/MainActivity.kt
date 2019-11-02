package de.yochyo.yummybooru.layout

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import de.yochyo.eventmanager.EventCollection
import de.yochyo.subeventcollection.SubEventCollection
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.downloads.cache
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.Database
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.UpdateServersEvent
import de.yochyo.yummybooru.events.events.UpdateTagsEvent
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddSpecialTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.layout.alertdialogs.ConfirmDialog
import de.yochyo.yummybooru.layout.res.Menus
import de.yochyo.yummybooru.updater.AutoUpdater
import de.yochyo.yummybooru.updater.Changelog
import de.yochyo.yummybooru.utils.setColor
import de.yochyo.yummybooru.utils.toTagString
import de.yochyo.yummybooru.utils.underline
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
    private val filteredTags = ArrayList<Pair<EventCollection<Tag>, String>>()
        get() {
            if (field.isEmpty()) field += Pair(db.tags, "")
            return field
        }
    private val currentFilter: EventCollection<Tag> get() = filteredTags.last().first

    companion object {
        val selectedTags = ArrayList<String>()
    }

    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var tagAdapter: SearchTagAdapter
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
        Database.initDatabase(this)

        UpdateTagsEvent.registerListener { tagAdapter.notifyDataSetChanged() }
        UpdateServersEvent.registerListener { serverAdapter.notifyDataSetChanged() }

        tagAdapter = SearchTagAdapter().apply { tagRecyclerView.adapter = this }
        val serverRecyclerView = findViewById<RecyclerView>(R.id.server_recycler_view)
        serverRecyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        serverAdapter = ServerAdapter().apply { serverRecyclerView.adapter = this }

        Changelog.showChangelogIfChanges(this)
        AutoUpdater().autoUpdate(this)

    }

    private fun initDrawerToolbar(toolbar: androidx.appcompat.widget.Toolbar) {
        toolbar.inflateMenu(R.menu.main_search_nav_menu)
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.search -> {
                        drawer_layout.closeDrawer(GravityCompat.END)
                        PreviewActivity.startActivity(this, if (selectedTags.isEmpty()) "*" else selectedTags.toTagString())
                    }
                    R.id.add_tag -> {
                        AddTagDialog {
                            GlobalScope.launch {
                                val tag = Api.getTag(it.text.toString())
                                val t = db.addTag(this@MainActivity, tag)
                                withContext(Dispatchers.Main) {
                                    tagLayoutManager.scrollToPositionWithOffset(db.tags.indexOf(t), 0)
                                }
                            }
                        }.build(this)
                    }
                    R.id.add_special_tag -> {
                        AddSpecialTagDialog().build(this)
                    }
                    else -> return@setOnMenuItemClickListener false
                }
                return@setOnMenuItemClickListener true
            }

    }

    private inner class SearchTagAdapter : RecyclerView.Adapter<SearchTagViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchTagViewHolder = SearchTagViewHolder((layoutInflater.inflate(R.layout.search_item_layout, parent, false) as Toolbar)).apply {
            toolbar.inflateMenu(R.menu.activity_main_search_menu)
            val check = toolbar.findViewById<CheckBox>(R.id.search_checkbox)

            fun onClick() {
                if (check.isChecked) selectedTags.add(toolbar.findViewById<TextView>(R.id.search_textview).text.toString())
                else selectedTags.remove(toolbar.findViewById<TextView>(R.id.search_textview).text)
            }
            toolbar.setOnClickListener {
                check.isChecked = !check.isChecked
                onClick()
            }
            check.setOnClickListener {
                onClick()
            }

            toolbar.setOnMenuItemClickListener {
                val tag = currentFilter.elementAt(adapterPosition)
                when (it.itemId) {
                    R.id.main_search_favorite_tag -> GlobalScope.launch {
                        val copy = tag.copy(isFavorite = !tag.isFavorite)
                        db.changeTag(this@MainActivity, copy)
                    }
                    R.id.main_search_subscribe_tag -> {
                        GlobalScope.launch {
                            if (db.getSubscription(tag.name) == null) db.addSubscription(this@MainActivity, Subscription.fromTag(tag))
                            else db.deleteSubscription(this@MainActivity, tag.name)
                            withContext(Dispatchers.Main) { notifyItemChanged(adapterPosition) }
                        }
                    }
                    R.id.main_search_delete_tag -> {
                        GlobalScope.launch {
                            selectedTags.remove(tag.name)
                            db.deleteTag(this@MainActivity, tag.name)
                        }
                    }
                }
                true
            }
        }

        override fun onBindViewHolder(holder: SearchTagViewHolder, position: Int) {
            val tag = currentFilter.elementAt(position)
            holder.toolbar.findViewById<CheckBox>(R.id.search_checkbox).isChecked = selectedTags.contains(tag.name)
            val textView = holder.toolbar.findViewById<TextView>(R.id.search_textview)
            textView.text = tag.name;textView.setColor(tag.color);textView.underline(tag.isFavorite)
            Menus.initMainSearchTagMenu(holder.toolbar.menu, tag)
        }

        override fun getItemCount(): Int = currentFilter.size
    }

    private inner class ServerAdapter : RecyclerView.Adapter<ServerViewHolder>() {
        override fun getItemCount(): Int = db.servers.size

        private fun longClickDialog(server: Server) {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setItems(arrayOf(getString(R.string.edit_server), getString(R.string.delete_server))) { dialog, i ->
                dialog.cancel()
                when (i) {
                    0 -> editServerDialog(server)
                    1 -> {
                        if (!server.isSelected) ConfirmDialog { server.deleteServer(this@MainActivity) }.withTitle(getString(R.string.delete) + " [${server.name}]").build(this@MainActivity)
                        else Toast.makeText(this@MainActivity, getString(R.string.cannot_delete_server), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            builder.show()
        }

        private fun editServerDialog(server: Server) {
            AddServerDialog {
                GlobalScope.launch {
                    db.changeServer(this@MainActivity, it)
                    withContext(Dispatchers.Main) { Snackbar.make(drawer_layout, "Edit [${it.name}]", Snackbar.LENGTH_SHORT).show() }
                }
            }.withServer(server).withTitle(getString(R.string.edit_server)).build(this@MainActivity)
        }

        override fun onCreateViewHolder(parent: ViewGroup, position: Int): ServerViewHolder {
            val holder = ServerViewHolder((layoutInflater.inflate(R.layout.server_item_layout, parent, false) as LinearLayout))
            holder.layout.setOnClickListener {
                val server = db.servers.elementAt(holder.adapterPosition)
                GlobalScope.launch(Dispatchers.Main) {
                    server.select(this@MainActivity)
                }
            }
            holder.layout.setOnLongClickListener {
                val server = db.servers.elementAt(holder.adapterPosition)
                longClickDialog(server)
                true
            }
            return holder
        }

        override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
            val server = db.servers.elementAt(position)
            fillServerLayoutFields(holder.layout, server, server.isSelected)
        }

        fun fillServerLayoutFields(layout: LinearLayout, server: Server, isSelected: Boolean = false) {
            val text1 = layout.findViewById<TextView>(R.id.server_text1)
            text1.text = server.name
            if (isSelected) text1.setColor(R.color.dark_red)
            else text1.setColor(R.color.violet)
            layout.findViewById<TextView>(R.id.server_text2).text = server.api
            layout.findViewById<TextView>(R.id.server_text3).text = server.userName
        }
    }

    private val filterMutex = Mutex()
    suspend fun filter(name: String) {
        withContext(Dispatchers.Default) {
            filterMutex.withLock {
                var addChild: EventCollection<Tag>
                if (name == "") {
                    for (item in filteredTags) {
                        val list = item.first
                        if (list is SubEventCollection) list.destroy()
                    }
                    filteredTags.clear()
                    addChild = db.tags
                } else {
                    for (i in filteredTags.indices.reversed()) {
                        if (name.startsWith(filteredTags[i].second)) {
                            val subCollection = SubEventCollection(TreeSet(), filteredTags[i].first) { it.name.contains(name) }
                            addChild = subCollection
                            break
                        }
                    }
                    addChild = SubEventCollection(TreeSet(), db.tags) { it.name.contains(name) }
                }
                withContext(Dispatchers.Main) {
                    tagLayoutManager.scrollToPosition(0)
                    tagAdapter.notifyDataSetChanged()
                    filteredTags += Pair(addChild, name)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_server -> AddServerDialog {
                GlobalScope.launch {
                    db.addServer(this@MainActivity, it)
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

    private inner class ServerViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)
    private inner class SearchTagViewHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}
