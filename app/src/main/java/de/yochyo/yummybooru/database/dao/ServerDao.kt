package de.yochyo.yummybooru.database.dao

import androidx.room.*
import de.yochyo.yummybooru.api.entities.Server

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers")
    fun selectAll(): List<Server>

    @Query("SELECT * FROM servers WHERE id = :id")
    fun select(id: Int): Server

    @Delete
    fun delete(server: Server)

    @Insert //autogenerates id if id is 0
    fun insert(server: Server): Long

    @Update
    fun update(server: Server)
}