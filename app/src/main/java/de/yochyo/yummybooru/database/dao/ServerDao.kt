package de.yochyo.yummybooru.database.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import de.yochyo.yummybooru.api.entities.Server

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers")
    fun selectAll(): LiveData<List<Server>>

    @Query("SELECT * FROM servers")
    fun selectAllNoFlow(): List<Server>

    @Delete
    fun delete(server: Server): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(server: Server): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(servers: List<Server>): List<Long>

    @Update(onConflict = OnConflictStrategy.IGNORE)
    fun update(server: Server): Int
}