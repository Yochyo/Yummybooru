package de.yochyo.ybooru.layout

import android.Manifest
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.*
import de.yochyo.ybooru.R
import de.yochyo.ybooru.api.Tag
import de.yochyo.ybooru.database
import de.yochyo.ybooru.layout.res.Menus
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.toTagString
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var menu: Menu

    private val selectedTags = ArrayList<String>()

    private lateinit var recycleView: RecyclerView
    private lateinit var adapter: Adapter
    private lateinit var layoutmanager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        initDrawer()
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)

        val searchHeader = nav_search.getHeaderView(0)
        recycleView = searchHeader.findViewById(R.id.recycler_view_search)
        adapter = Adapter().apply { recycleView.adapter = this }
        layoutmanager = LinearLayoutManager(this).apply { recycleView.layoutManager = this }
        initAddTagButton(searchHeader.findViewById(R.id.add_search))
        initSearchButton(searchHeader.findViewById(R.id.start_search))
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        menuInflater.inflate(R.menu.main_menu, menu)
        setMenuR18Text()
        Manager.resetAll()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_r18 -> {
                database.r18 = !database.r18
                setMenuR18Text()
                Manager.resetAll()
                return true
            }
            R.id.search -> {
                drawer_layout.openDrawer(GravityCompat.END)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }

    private fun initDrawer() {
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun initAddTagButton(b: Button) {
        b.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val layout = LayoutInflater.from(this).inflate(R.layout.search_item_dialog_view, null) as LinearLayout
            val editText = layout.findViewById<EditText>(R.id.add_tag_edittext)
            builder.setMessage("Add Tag").setPositiveButton("OK") { _, _ ->
                val tag = database.addTag(editText.text.toString(), Tag.UNKNOWN, false)
                adapter.notifyItemInserted(database.getTags().lastIndex)
            }
            builder.setView(layout)
            val dialog = builder.create()
            dialog.show()
            editText.requestFocus()
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        }
    }

    private fun initSearchButton(b: Button) {
        b.setOnClickListener {
            drawer_layout.closeDrawer(GravityCompat.END)
            PreviewActivity.startActivity(this, selectedTags.toTagString())
        }
    }

    private inner class Adapter : RecyclerView.Adapter<SearchItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder = SearchItemViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.search_item_layout, parent, false) as Toolbar)).apply {
            toolbar.setOnClickListener {
                val check = it.findViewById<CheckBox>(R.id.search_checkbox)
                if (check.isChecked) selectedTags.remove(findViewById<TextView>(R.id.search_textview).text)
                else selectedTags.add(findViewById<TextView>(R.id.search_textview).text.toString())
                check.isChecked = !check.isChecked
            }
            toolbar.inflateMenu(R.menu.activity_main_search_menu)
            toolbar.setOnMenuItemClickListener {
                val tag = database.getTags()[adapterPosition].apply { isFavorite = !isFavorite }
                when (it.itemId) {
                    R.id.main_search_favorite_tag -> {
                        database.changeTag(tag)
                        adapter.notifyItemChanged(adapterPosition)
                    }
                    R.id.main_search_subscribe_tag -> {
                        if (database.getSubscription(tag.name) == null) {
                            database.addSubscription(tag.name, 0)//TODO
                            Toast.makeText(this@MainActivity, "Subscribed ${tag.name}", Toast.LENGTH_SHORT).show()
                        } else {
                            database.removeSubscription(tag.name)
                            Toast.makeText(this@MainActivity, "Unsubscribed ${tag.name}", Toast.LENGTH_SHORT).show()
                        }
                        adapter.notifyItemChanged(adapterPosition)
                    }
                    R.id.main_search_delete_tag -> {
                        database.removeTag(tag.name)
                        adapter.notifyItemRemoved(adapterPosition)
                    }
                }
                true
            }
        }

        override fun getItemCount(): Int = database.getTags().size
        override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
            val tag = database.getTags()[position]
            val textView = holder.toolbar.findViewById<TextView>(R.id.search_textview)
            Menus.initMainSearchTagMenu(this@MainActivity, holder.toolbar.menu, tag)
            if (tag.isFavorite) textView.paintFlags = textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            textView.text = tag.name
        }
    }

    private inner class SearchItemViewHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}
