package de.yochyo.yummybooru.database.updates

import android.database.sqlite.SQLiteDatabase
import de.yochyo.yummybooru.database.utils.Upgrade

class Upgrade2 : Upgrade(2) {
    override fun upgrade(sql: SQLiteDatabase) {
        sql.execSQL("ALTER TABLE tags RENAME TO tags_old")
        sql.execSQL("ALTER TABLE subs RENAME TO subs_old")
        sql.execSQL("ALTER TABLE servers RENAME TO servers_old")


        sql.execSQL("CREATE TABLE IF NOT EXISTS servers(name TEXT NOT NULL, url TEXT NOT NULL, api TEXT NOT NULL, username TEXT NOT NULL, password TEXT NOT NULL, id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT)")
        sql.execSQL("CREATE TABLE IF NOT EXISTS tags(name TEXT NOT NULL, type INTEGER NOT NULL, server_id INTEGER NOT NULL, isFavorite INTEGER NOT NULL, creation INTEGER NOT NULL, last_count INTEGER, last_id INTEGER, PRIMARY KEY(name, server_id), FOREIGN KEY(server_id) REFERENCES servers(id) ON UPDATE CASCADE ON DELETE CASCADE)")
        sql.execSQL("INSERT OR IGNORE INTO servers SELECT name, url, api, userName, password, id FROM servers_old")
        sql.execSQL("INSERT OR IGNORE INTO tags SELECT t.name, t.type, s.id, case when subs.isFavorite IS NULL then t.isFavorite when subs.isFavorite > t.isFavorite then subs.isFavorite else t.isFavorite end, t.creation, subs.lastCount, subs.lastID FROM tags_old t, servers_old s LEFT JOIN subs_old subs ON (t.name = subs.name AND t.serverID = subs.serverID) WHERE t.serverID = s.id")
        sql.execSQL("INSERT OR IGNORE INTO tags SELECT sub.name, sub.type, s.id, sub.isFavorite, sub.creation, sub.lastCount, sub.lastID FROM subs_old sub, servers_old s WHERE sub.serverID = s.id")

        sql.execSQL("DROP TABLE tags_old")
        sql.execSQL("DROP TABLE subs_old")
        sql.execSQL("DROP TABLE servers_old")
    }

}