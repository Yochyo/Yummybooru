package de.yochyo.ybooru.layout

import android.Manifest
import android.arch.lifecycle.Observer
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.*
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Downloader
import de.yochyo.ybooru.api.api.Api
import de.yochyo.ybooru.api.api.DanbooruApi
import de.yochyo.ybooru.database.Database
import de.yochyo.ybooru.database.database
import de.yochyo.ybooru.database.entities.Subscription
import de.yochyo.ybooru.database.entities.Tag
import de.yochyo.ybooru.layout.alertdialogs.AddTagDialog
import de.yochyo.ybooru.layout.res.Menus
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.setColor
import de.yochyo.ybooru.utils.toTagString
import de.yochyo.ybooru.utils.underline
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private val selectedTags = ArrayList<String>()
    private lateinit var menu: Menu

    private lateinit var recycleView: RecyclerView
    private lateinit var adapter: SearchTagAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Database.initDatabase(this)
        Api.instance = DanbooruApi(database.currentServer!!.url)//TODO
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

        val navLayout = nav_search.findViewById<LinearLayout>(R.id.nav_search_layout)
        initAddTagButton(navLayout.findViewById(R.id.add_search))
        initSearchButton(navLayout.findViewById(R.id.start_search))
        recycleView = navLayout.findViewById(R.id.recycler_view_search)
        recycleView.layoutManager = LinearLayoutManager(this)
        adapter = SearchTagAdapter().apply { recycleView.adapter = this }
        database.tags.observe(this, Observer<TreeSet<Tag>> { t -> if (t != null) adapter.updateTags(t) })
    }

    private fun initAddTagButton(b: Button) {
        b.setOnClickListener {
            AddTagDialog {
                if (database.getTag(it.text.toString()) == null) {
                    GlobalScope.launch {
                        val tag = Api.getTag(it.text.toString())
                        launch(Dispatchers.Main) {
                            val newTag: Tag = tag ?: Tag(it.text.toString(), Tag.UNKNOWN, false)
                            database.addTag(newTag)
                        }
                    }
                }
            }.apply { title = "Add Subscription" }.build(this)
        }
    }

    private fun initSearchButton(b: Button) {
        b.setOnClickListener {
            drawer_layout.closeDrawer(GravityCompat.END)
            if (selectedTags.isEmpty()) PreviewActivity.startActivity(this, "*")
            else PreviewActivity.startActivity(this, selectedTags.toTagString())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.main_menu, menu)
        setMenuR18Text()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_r18 -> {
                database.r18 = !database.r18
                setMenuR18Text()
                Manager.resetAll()
            }
            R.id.search -> drawer_layout.openDrawer(GravityCompat.END)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_subs -> startActivity(Intent(this, SubscriptionActivity::class.java))
            R.id.nav_settings -> startActivity(Intent(this, SettingsActivity::class.java))
            R.id.community -> Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show()
            R.id.nav_help -> Toast.makeText(this, "Ask me some questions", Toast.LENGTH_SHORT).show()
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return super.onOptionsItemSelected(item)
    }

    private fun setMenuR18Text() {
        if (database.r18) menu.findItem(R.id.action_r18).title = getString(R.string.enter_r18)
        else menu.findItem(R.id.action_r18).title = getString(R.string.leave_r18)
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) drawer_layout.closeDrawer(GravityCompat.START)
        else if (drawer_layout.isDrawerOpen(GravityCompat.END)) drawer_layout.closeDrawer(GravityCompat.END)
        else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        Downloader.getInstance(this).clearCache()
    }

    private inner class SearchTagAdapter : RecyclerView.Adapter<SearchTagViewHolder>() {
        private var tags = TreeSet<Tag>()

        fun updateTags(set: TreeSet<Tag>) {
            tags = set
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchTagViewHolder = SearchTagViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.search_item_layout, parent, false) as Toolbar)).apply {
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
                    R.id.main_search_favorite_tag -> database.changeTag(tag.apply { isFavorite = !isFavorite })
                    R.id.main_search_subscribe_tag -> {
                        if (database.getSubscription(tag.name) == null) {
                            GlobalScope.launch { val currentID = Api.newestID();launch(Dispatchers.Main) { database.addSubscription(Subscription(tag.name, tag.type, currentID)) } }
                            Toast.makeText(this@MainActivity, "Subscribed ${tag.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            database.deleteSubscription(tag.name)
                            Toast.makeText(this@MainActivity, "Unsubscribed ${tag.name}", Toast.LENGTH_SHORT).show()
                        }
                        notifyItemChanged(adapterPosition)
                    }
                    R.id.main_search_delete_tag -> {
                        database.deleteTag(tag.name)
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

    private inner class SearchTagViewHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}
