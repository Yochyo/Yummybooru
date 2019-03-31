package de.yochyo.ybooru.database.entities

import android.arch.persistence.room.*
import java.util.*

@Entity(tableName = "servers")
class Server(var name: String, var api: String, var userName: String = "", var passwordHash: String = "", val creation: Date = Date(), @PrimaryKey(autoGenerate = true) val id: Int = 0) : Comparable<Server> {
    override fun compareTo(other: Server): Int {
        return creation.compareTo(other.creation)
    }

    override fun toString(): String {
        return name
    }
}

@Dao
interface ServerDao {
    @Insert
    fun insert(server: Server)

    @Delete
    fun delete(server: Server)

    @Update
    fun update(server: Server)

    @Query("SELECT * FROM servers")
    fun getAllServers(): List<Server>
}