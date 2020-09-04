package de.yochyo.yummybooru.backup

import android.content.Context
import de.yochyo.json.JSONObject

object PreferencesBackup : BackupableEntity<String> {
    override fun toJSONObject(e: String, context: Context): JSONObject {
        val prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)
        val json = JSONObject()
        val strings = JSONObject()
        val integers = JSONObject()
        val longs = JSONObject()
        val booleans = JSONObject()
        val floats = JSONObject()
        json.put("strings", strings)
        json.put("integers", integers)
        json.put("longs", longs)
        json.put("booleans", booleans)
        json.put("floats", floats)

        for (pref in prefs.all) {
            if (pref.value is String) strings.put(pref.key, pref.value)
            if (pref.value is Int) integers.put(pref.key, pref.value)
            if (pref.value is Long) longs.put(pref.key, pref.value)
            if (pref.value is Boolean) booleans.put(pref.key, pref.value)
            if (pref.value is Float) floats.put(pref.key, pref.value)
        }
        return json
    }

    override suspend fun restoreEntity(json: JSONObject, context: Context) {
        val prefs = context.getSharedPreferences("default", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            val strings = json.getJSONObject("strings")
            val integers = json.getJSONObject("integers")
            val longs = json.getJSONObject("longs")
            val booleans = json.getJSONObject("booleans")
            val floats = json.getJSONObject("floats")
            for (key in strings.keys()) putString(key, strings.getString(key))
            for (key in integers.keys()) putInt(key, integers.getInt(key))
            for (key in longs.keys()) putLong(key, longs.getLong(key))
            for (key in booleans.keys()) putBoolean(key, booleans.getBoolean(key))
            for (key in floats.keys()) putFloat(key, floats.getFloat(key))
            apply()
        }
    }
}