package de.yochyo.yummybooru.layout.menus

import android.view.MenuItem
import com.google.android.material.navigation.NavigationView

typealias OnClickItem = (menuItem: MenuItem) -> Boolean

class SettingsNavView(val navView: NavigationView) : NavigationView.OnNavigationItemSelectedListener {
    private val menuItemOnClickEvents = HashMap<Int, OnClickItem>()
    private var onItemSelected: OnClickItem = { false }

    fun inflateMenu(id: Int, onItemSelected: OnClickItem) {
        navView.menu.clear()
        navView.inflateMenu(id)
        this.onItemSelected = onItemSelected
    }

    fun addMenuItem(name: String, onItemSelected: OnClickItem) {
        val menuItem = navView.menu.add(name)
        menuItemOnClickEvents[menuItem.itemId] = onItemSelected
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (!onItemSelected(item)){
            menuItemOnClickEvents.keys.forEach {
                if (item.itemId == it) {
                    menuItemOnClickEvents[it]!!(item)
                    return false
                }
            }
        }
        return false
    }
}