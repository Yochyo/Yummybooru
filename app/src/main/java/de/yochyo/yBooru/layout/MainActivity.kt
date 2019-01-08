package de.yochyo.yBooru.layout

import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Toolbar
import de.yochyo.yBooru.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    val dataSet = ArrayList<String>().apply { add("1");add("2");add("31");add("4");add("5");add("6");add("7");add("8");add("9");add("10");add("11");add("12");add("13");add("14");add("15");add("16");add("17");add("18");add("19") }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)


        val searchView = nav_search.getHeaderView(0).findViewById<RecyclerView>(R.id.recycler_view_search)
        searchView.adapter = Adapter()
        searchView.layoutManager = LinearLayoutManager(this)
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
                return true
            }
            R.id.search -> {
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


    private inner class Adapter : RecyclerView.Adapter<SearchItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemViewHolder = SearchItemViewHolder((LayoutInflater.from(parent.context).inflate(R.layout.search_item_layout, parent, false) as Toolbar).apply { inflateMenu(R.menu.activity_main_search_menu) })

        override fun getItemCount(): Int = dataSet.size
        override fun onBindViewHolder(holder: SearchItemViewHolder, position: Int) {
        }
    }

    private inner class SearchItemViewHolder(val toolbar: Toolbar) : RecyclerView.ViewHolder(toolbar)
}
