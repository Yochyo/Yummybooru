package de.yochyo.ybooru.backup

import android.content.Context
import de.yochyo.ybooru.database.db
import de.yochyo.ybooru.utils.configPath
import java.io.File

object BackupUtils {
    val directory = "$configPath/backup"

    val dir = File(directory)

    init {
        dir.mkdirs()
    }

    fun createBackup(context: Context) {
        val f = createBackupFile()
        val builder = StringBuilder()
        builder.append("--Tag--\n")
        for (tag in db.tags.value!!)
            builder.append(TagBackup.toString(tag, context) + "\n")
        builder.append("--Sub--\n")
        for (sub in db.subs.value!!)
            builder.append(SubscriptionBackup.toString(sub, context) + "\n")
        builder.append("--Server--\n")
        for (server in db.servers.value!!)
            builder.append(ServerBackup.toString(server, context) + "\n")
        builder.append("--Preferences--\n")
        builder.append(PreferencesBackup.toString("", context) + "\n")
        f.writeBytes(builder.toString().toByteArray())
    }

    suspend fun restoreBackup(byteArray: ByteArray, context: Context) {
        fun restore(type: String, line: String) {
            when (type) {
                "Tag" -> TagBackup.toEntity(line, context)
                "Sub" -> SubscriptionBackup.toEntity(line, context)
                "Server" -> ServerBackup.toEntity(line, context)
                "Preferences" -> PreferencesBackup.toEntity(line, context)
                else -> throw Exception("type [$type] does not exist")
            }
        }
        try {
            val map = HashMap<String, ArrayList<String>>()//type, lines
            val s = String(byteArray)
            val lines = s.split("\n")
            var currentType = "-1"
            for (line in lines) {
                if (line.startsWith("--") && line.endsWith("--")) {
                    currentType = line.substring(2, line.length - 2)
                    continue
                }
                if (line != "") {
                    if (map[currentType] == null)
                        map[currentType] = ArrayList(50)
                    map[currentType]!!.add(line)
                }
            }
            //Wenn es keine Fehler gibt, wiederherstellen
            db.deleteEverything()
            for (entry in map) {
                for (v in entry.value)
                    restore(entry.key, v)
            }
        } catch (e: Exception) {
        }
    }

    private fun createBackupFile(): File {
        val file = File("$directory/backup" + System.currentTimeMillis() + ".yBooru")
        file.createNewFile()
        return file
    }
}