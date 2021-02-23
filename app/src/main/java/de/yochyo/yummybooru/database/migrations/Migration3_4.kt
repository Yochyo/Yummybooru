package de.yochyo.yummybooru.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration3_4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `tags2` (`id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, `isFavorite` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` INTEGER NOT NULL, `creation` INTEGER NOT NULL, `server_id` INTEGER NOT NULL, `last_id` INTEGER, `last_count` INTEGER, CONSTRAINT uniq UNIQUE(name, server_id), FOREIGN KEY(`server_id`) REFERENCES `servers`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )")
        db.execSQL("INSERT INTO tags2 (isFavorite, name, type, creation, server_id, last_id, last_count) SELECT isFavorite, name, type, creation, server_id, last_id, last_count FROM tags;")
        db.execSQL("DROP TABLE tags")
        db.execSQL("ALTER TABLE tags2 RENAME TO tags")
    }
}