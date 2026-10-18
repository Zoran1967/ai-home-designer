package com.zoran.aihomedesigner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OpeningDao {

    /** Svi otvori svih soba jednog projekta. */
    @Query(
        "SELECT openings.* FROM openings " +
            "INNER JOIN rooms ON rooms.id = openings.roomId " +
            "WHERE rooms.projectId = :projectId " +
            "ORDER BY openings.createdAtMillis ASC"
    )
    fun observeByProject(projectId: String): Flow<List<OpeningEntity>>

    @Query("SELECT * FROM openings WHERE roomId = :roomId")
    suspend fun getByRoom(roomId: String): List<OpeningEntity>

    @Query("SELECT * FROM openings WHERE id = :id")
    suspend fun getById(id: String): OpeningEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(opening: OpeningEntity)

    @Update
    suspend fun update(opening: OpeningEntity)

    @Query("DELETE FROM openings WHERE id = :id")
    suspend fun deleteById(id: String)
}
