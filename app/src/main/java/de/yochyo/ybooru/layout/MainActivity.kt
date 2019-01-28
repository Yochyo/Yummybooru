package de.yochyo.ybooru.layout

import android.Manifest
import android.content.Intent
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
import de.yochyo.ybooru.manager.Manager
import de.yochyo.ybooru.utils.toTagString
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var menu: Menu

    private val dataSet = ArrayList<Tag>()
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

        dataSet += database.getTags()
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
            R.id.nav_help -> {
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
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
                if (tag != null) {
                    dataSet.add(tag)
                    adapter.notifyItemInserted(dataSet.lastIndex)
                    database.addTag(editText.text.toString(), Tag.UNKNOWN, false)
                }
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
            val intent = Intent(this, PreviewActivity::class.java)
            intent.putExtra("tags", selectedTags.toTagString())
            startActivity(intent)
        }
    }

    private inner class Adapter : RecyclerView.Adapter<SearchItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder = SearchItemViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.search_item_layout, parent, false) as Toolbar).apply {
            inflateMenu(R.menu.activity_main_search_menu)
            setOnClickListener {
                val check = findViewById<CheckBox>(R.id.search_checkbox)
                if (check.isChecked) selectedTags.remove(findViewById<TextView>(R.id.search_textview).text)
                else selectedTags.add(findViewById<TextView>(R.id.search_textview).text.toString())
                check.isChecked = !check.isChecked
            }
        })

        override fun getItemCount(): Int = dataSet.size
        override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
            holder.toolbar.findViewById<TextView>(R.id.search_textview).text = dataSet[position].name
        }
    }
    private inner class SearchItemViewHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}
