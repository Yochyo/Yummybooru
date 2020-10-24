package de.yochyo.yummybooru.api.manager

import android.content.Context
import de.yochyo.yummybooru.utils.distributor.Distributor

object ManagerDistributor : Distributor<String, ManagerWrapper>() {
    override fun createIfNotExist(context: Context, key: String): ManagerWrapper {
        return ManagerWrapper.build(context, key)
    }
}