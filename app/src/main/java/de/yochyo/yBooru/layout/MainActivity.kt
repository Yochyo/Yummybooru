package de.yochyo.yBooru.layout

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.*
import de.yochyo.yBooru.R
import de.yochyo.yBooru.Tag
import de.yochyo.yBooru.api.Api
import de.yochyo.yBooru.database
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
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


        dataSet += database.getTags()
        initIsFilteredText()
        val searchHeader = nav_search.getHeaderView(0)
        recycleView = searchHeader.findViewById(R.id.recycler_view_search)
        adapter = Adapter().apply { recycleView.adapter = this }
        layoutmanager = LinearLayoutManager(this).apply { recycleView.layoutManager = this }
        initAddTagButton(searchHeader.findViewById(R.id.add_search))
        initSearchButton(searchHeader.findViewById(R.id.start_search))
    }


    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_r18 -> {
                Api.safeSearch = !Api.safeSearch
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


    private fun initAddTagButton(b: Button) {
        b.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val layout = LayoutInflater.from(this).inflate(R.layout.search_item_dialog_view, null) as LinearLayout
            val editText = layout.findViewById<EditText>(R.id.add_tag_edittext)
            builder.setMessage("Add Tag").setPositiveButton("OK") { _, _ ->
                val tag = database.addTag(editText.text.toString(), false)
                if (tag != null) {
                    dataSet.add(tag)
                    adapter.notifyItemInserted(dataSet.lastIndex)
                    database.addTag(editText.text.toString(), false)
                }
            }.setNegativeButton("CANCEL") { _, _ -> }
            builder.setView(layout)
            builder.create().show()
        }
    }

    private fun initIsFilteredText() {
        if (database.r18)
            toolbar.menu.findItem(R.id.action_r18).title = getString(R.string.enter_r18)
        else
            toolbar.menu.findItem(R.id.action_r18).title = getString(R.string.leave_r18)
    }

    private fun initDrawer() {
        val toggle = ActionBarDrawerToggle(this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
    }

    private fun initSearchButton(b: Button) {
        b.setOnClickListener {
            val intent = Intent(this, PreviewActivity::class.java)
            intent.putExtra("tags", selectedTags.joinToString(" "))
            startActivity(intent)
        }
    }

    private inner class Adapter : RecyclerView.Adapter<SearchItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder = SearchItemViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.search_item_layout, parent, false) as Toolbar)
                .apply {
                    inflateMenu(R.menu.activity_main_search_menu)
                    setOnClickListener {
                        val check = findViewById<CheckBox>(R.id.search_checkbox)
                        if (check.isChecked) {
                            check.isChecked = false
                            selectedTags.remove(findViewById<TextView>(R.id.search_textview).text)
                        } else {
                            check.isChecked = true
                            selectedTags.add(findViewById<TextView>(R.id.search_textview).text.toString())
                        }
                    }
                })

        override fun getItemCount(): Int = dataSet.size
        override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
            holder.toolbar.findViewById<TextView>(R.id.search_textview).text = dataSet[position].name
        }
    }

    private inner class SearchItemViewHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}
