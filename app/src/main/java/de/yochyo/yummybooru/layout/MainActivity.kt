package de.yochyo.yummybooru.layout

import android.Manifest
import android.arch.lifecycle.Observer
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.*
import de.yochyo.yummybooru.R
import de.yochyo.yummybooru.api.api.Api
import de.yochyo.yummybooru.api.api.DanbooruApi
import de.yochyo.yummybooru.api.api.MoebooruApi
import de.yochyo.yummybooru.api.downloads.cache
import de.yochyo.yummybooru.api.entities.Server
import de.yochyo.yummybooru.api.entities.Subscription
import de.yochyo.yummybooru.api.entities.Tag
import de.yochyo.yummybooru.database.Database
import de.yochyo.yummybooru.database.db
import de.yochyo.yummybooru.events.events.*
import de.yochyo.yummybooru.events.listeners.*
import de.yochyo.yummybooru.layout.alertdialogs.AddServerDialog
import de.yochyo.yummybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.yummybooru.layout.res.Menus
import de.yochyo.yummybooru.utils.setColor
import de.yochyo.yummybooru.utils.toTagString
import de.yochyo.yummybooru.utils.underline
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    companion object {
        val selectedTags = ArrayList<String>()
    }

    private lateinit var tagRecyclerView: RecyclerView
    private lateinit var tagAdapter: SearchTagAdapter
    private lateinit var serverAdapter: ServerAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        initListeners()
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
        initAddTagButton(navLayout.findViewById(R.id.add_search))
        initSearchButton(navLayout.findViewById(R.id.start_search))
        tagRecyclerView = navLayout.findViewById(R.id.recycler_view_search)
        tagRecyclerView.layoutManager = LinearLayoutManager(this)
        tagAdapter = SearchTagAdapter().apply { tagRecyclerView.adapter = this }
        val serverRecyclerView = findViewById<RecyclerView>(R.id.server_recycler_view)
        serverRecyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
        serverAdapter = ServerAdapter().apply { serverRecyclerView.adapter = this }

        if (hasPermission) initData()
    }

    private fun initListeners() {
        AddTagEvent.registerListener(DisplayToastAddTagListener())
        AddSubEvent.registerListener(DisplayToastAddSubListener())
        AddServerEvent.registerListener(DisplayToastAddServerListener())
        DeleteServerEvent.registerListener(DisplayToastDeleteServerListener())
        DeleteSubEvent.registerListener(DisplayToastDeleteSubListener())
        DeleteTagEvent.registerListener(DisplayToastDeleteTagListener())
        ChangeSubEvent.registerListener(DisplayToastFavoriteSubListener())
        ChangeTagEvent.registerListener(DisplayToastFavoriteTagListener())
        DeleteTagEvent.registerListener(RemoveSelectedTagsInMainactivityListener())
        ChangeServerEvent.registerListener(DisplayToastChangeServerEvent())
        SelectServerEvent.registerListener(DisplayToastSelectServerListener())
        SelectServerEvent.registerListener(ClearSelectedTagsInMainactivityListener())
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.none { it != PackageManager.PERMISSION_GRANTED })
            initData()
    }

    fun initData() {
        GlobalScope.launch { cache.clearCache() }
        Api.addApi(DanbooruApi(""))
        Api.addApi(MoebooruApi(""))
        Database.initDatabase(this)

        db.tags.observe(this, Observer<TreeSet<Tag>> { t -> if (t != null) tagAdapter.updateTags(t) })
        db.servers.observe(this, Observer<TreeSet<Server>> { s -> if (s != null) serverAdapter.updateServers(s) })
    }

    private fun initAddTagButton(b: Button) {
        b.setOnClickListener {
            AddTagDialog {
                GlobalScope.launch {
                    val tag = Api.getTag(it.text.toString())
                    launch(Dispatchers.Main) {
                        val t = db.addTag(this@MainActivity, tag ?: Tag(it.text.toString(), Tag.UNKNOWN))
                        tagRecyclerView.layoutManager?.scrollToPosition(db.tags.indexOf(t))
                    }
                }
            }.apply { title = getString(R.string.add_tag) }.build(this)
        }
    }

    private fun initSearchButton(b: Button) {
        b.setOnClickListener {
            drawer_layout.closeDrawer(GravityCompat.END)
            if (selectedTags.isEmpty()) PreviewActivity.startActivity(this, "*")
            else PreviewActivity.startActivity(this, selectedTags.toTagString())
        }
    }

    fun fillServerLayoutFields(layout: LinearLayout, server: Server, isSelected: Boolean = false) {
        val text1 = layout.findViewById<TextView>(R.id.server_text1)
        text1.text = server.name
        if (isSelected) text1.setColor(R.color.dark_red)
        else text1.setColor(R.color.violet)
        layout.findViewById<TextView>(R.id.server_text2).text = server.api
        layout.findViewById<TextView>(R.id.server_text3).text = server.userName

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_add_server -> AddServerDialog {
                GlobalScope.launch { db.addServer(this@MainActivity, it) }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) drawer_layout.closeDrawer(GravityCompat.START)
        else if (drawer_layout.isDrawerOpen(GravityCompat.END)) drawer_layout.closeDrawer(GravityCompat.END)
        else super.onBackPressed()
    }

    private inner class SearchTagAdapter : RecyclerView.Adapter<SearchTagViewHolder>() {
        private var tags = TreeSet<Tag>()

        fun updateTags(set: TreeSet<Tag>) {
            tags = set
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchTagViewHolder = SearchTagViewHolder((layoutInflater.inflate(R.layout.search_item_layout, parent, false) as Toolbar)).apply {
            val check = toolbar.findViewById<CheckBox>(R.id.search_checkbox)
            toolbar.inflateMenu(R.menu.activity_main_search_menu)
            toolbar.setOnClickListener {
                if (check.isChecked) selectedTags.remove(it.findViewById<TextView>(R.id.search_textview).text)
                else selectedTags.add(it.findViewById<TextView>(R.id.search_textview).text.toString())
                check.isChecked = !check.isChecked
            }
            check.setOnClickListener {
                if (!(it as CheckBox).isChecked) selectedTags.remove(toolbar.findViewById<TextView>(R.id.search_textview).text)
                else selectedTags.add(toolbar.findViewById<TextView>(R.id.search_textview).text.toString())
            }
            toolbar.setOnMenuItemClickListener {
                val tag = tags.elementAt(adapterPosition)
                when (it.itemId) {
                    R.id.main_search_favorite_tag -> GlobalScope.launch {
                        db.changeTag(this@MainActivity, tag.copy(isFavorite = !tag.isFavorite))
                        //  withContext(Dispatchers.Main){tagRecyclerView.layoutManager?.scrollToPosition(tags.indexOf(tag))}
                    }
                    R.id.main_search_subscribe_tag -> {
                        if (db.getSubscription(tag.name) == null) GlobalScope.launch { db.addSubscription(this@MainActivity, Subscription.fromTag(tag)) }
                        else GlobalScope.launch { db.deleteSubscription(this@MainActivity, tag.name) }
                        notifyItemChanged(adapterPosition)
                    }
                    R.id.main_search_delete_tag -> {
                        GlobalScope.launch { db.deleteTag(this@MainActivity, tag.name) }
                        selectedTags.remove(tag.name)
                    }
                }
                true
            }
        }

        override fun getItemCount(): Int = tags.size
        override fun onBindViewHolder(holder: SearchTagViewHolder, position: Int) {
            val tag = tags.elementAt(position)
            val check = holder.toolbar.findViewById<CheckBox>(R.id.search_checkbox)
            check.isChecked = selectedTags.contains(tag.name)
            val textView = holder.toolbar.findViewById<TextView>(R.id.search_textview)
            textView.text = tag.name
            textView.setColor(tag.color)
            textView.underline(tag.isFavorite)

            Menus.initMainSearchTagMenu(holder.toolbar.menu, tag)
        }
    }

    private inner class ServerAdapter : RecyclerView.Adapter<ServerViewHolder>() {
        var servers = TreeSet<Server>()
        override fun getItemCount(): Int = servers.size

        fun updateServers(servers: TreeSet<Server>) {
            this.servers = servers
            notifyDataSetChanged()
        }

        private fun longClickDialog(server: Server) {
            val builder = AlertDialog.Builder(this@MainActivity)
            builder.setItems(arrayOf(getString(R.string.edit_server), getString(R.string.delete_server))) { dialog, i ->
                dialog.cancel()
                when (i) {
                    0 -> editServerDialog(server)
                    1 -> {
                        if (!server.isSelected) deleteServerDialog(server)
                        else Toast.makeText(this@MainActivity, getString(R.string.cannot_delete_server), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            builder.show()
        }

        private fun editServerDialog(server: Server) {
            AddServerDialog {
                GlobalScope.launch { db.changeServer(this@MainActivity, it) }
            }.apply {
                serverID = server.id
                nameText = server.name
                apiText = server.api
                urlText = server.url
                userText = server.userName
                passwordText = server.password
                message = getString(R.string.edit_server)
                enableR18 = server.enableR18Filter
            }.build(this@MainActivity)
        }

        private fun deleteServerDialog(server: Server) {
            val b = AlertDialog.Builder(this@MainActivity)
            b.setTitle(R.string.delete)
            b.setMessage("${getString(R.string.do_you_want_to_delete_the_server)} [${server.name}]")
            b.setNegativeButton(getString(R.string.no)) { _, _ -> }
            b.setPositiveButton(getString(R.string.yes)) { _, _ -> server.deleteServer(this@MainActivity) }
            b.show()
        }

        override fun onCreateViewHolder(parent: ViewGroup, position: Int): ServerViewHolder {
            val holder = ServerViewHolder((layoutInflater.inflate(R.layout.server_item_layout, parent, false) as LinearLayout))
            holder.layout.setOnClickListener {
                val server = servers.elementAt(holder.adapterPosition)
                GlobalScope.launch(Dispatchers.Main) {
                    server.select(this@MainActivity)
                }
            }
            holder.layout.setOnLongClickListener {
                val server = servers.elementAt(holder.adapterPosition)
                longClickDialog(server)
                true
            }
            return holder
        }

        override fun onBindViewHolder(holder: ServerViewHolder, position: Int) {
            val server = servers.elementAt(position)
            fillServerLayoutFields(holder.layout, server, server.isSelected)
        }
    }

    private inner class ServerViewHolder(val layout: LinearLayout) : RecyclerView.ViewHolder(layout)
    private inner class SearchTagViewHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}
