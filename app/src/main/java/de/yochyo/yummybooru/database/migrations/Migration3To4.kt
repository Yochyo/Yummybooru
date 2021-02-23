package de.yochyo.yummybooru.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Migration3To4 : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tags2` (`isFavorite` INTEGER NOT NULL, `name` TEXT NOT NULL, `type` INTEGER NOT NULL, `creation` INTEGER NOT NULL, `server_id` " +
                    "INTEGER NOT NULL, `tagId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `last_id` INTEGER, `last_count` INTEGER, FOREIGN KEY(`server_id`) REFERENCES `servers`(`id`) ON UPDATE CASCADE ON DELETE CASCADE )"
        )

        db.execSQL("INSERT INTO tags2 (isFavorite, name, type, creation, server_id, last_id, last_count) SELECT isFavorite, name, type, creation, server_id, last_id, last_count FROM tags;")
        db.execSQL("DROP TABLE tags")
        db.execSQL("ALTER TABLE tags2 RENAME TO tags")

        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name_server_id` ON `tags` (`name`, `server_id`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `TagCollection` (`name` TEXT NOT NULL, `serverId` INTEGER NOT NULL, `collectionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, FOREIGN KEY(`serverId`) REFERENCES `servers`(`id`) ON UPDATE CASCADE ON DELETE CASCADE)"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_TagCollection_name_serverId` ON `TagCollection` (`name`, `serverId`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `TagCollectionTagCrossRef` (`collectionId` INTEGER NOT NULL, `tagId` INTEGER NOT NULL, PRIMARY KEY(`collectionId`, `tagId`))")
    }
}