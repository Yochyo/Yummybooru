package de.yochyo.yummybooru.database.converter

object ConvertBoolean {
    fun toBoolean(int: Int) = int == 1
    fun toInteger(boolean: Boolean) = if(boolean) 1 else 0
}