package de.yochyo.yummybooru.api.manager

import android.content.Context
import de.yochyo.yummybooru.utils.distributor.Distributor
import de.yochyo.yummybooru.utils.distributor.Pointer

object ManagerDistributor : Distributor<String, ManagerWrapper>() {
    override fun createIfNotExist(context: Context, key: String): ManagerWrapper {
        return ManagerWrapper.build(context, key)
    }

    override fun getPointer(context: Context, key: String): Pointer<ManagerWrapper> {
        val key = createIfNotExist(context, key).toString()
        return super.getPointer(context, key)
    }
}