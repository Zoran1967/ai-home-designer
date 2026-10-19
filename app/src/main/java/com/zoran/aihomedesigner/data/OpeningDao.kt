package com.zoran.aihomedesigner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface OpeningDao {

    @Query("SELECT * FROM openings WHERE roomId = :roomId ORDER BY createdAtMillis ASC")
    fun observeByRoom(roomId: String): Flow<List<OpeningEntity>>

    /** Svi otvori svih soba jednog projekta (za crtanje plana). */
    @Query(
        "SELECT o.* FROM openings o INNER JOIN rooms r ON o.roomId = r.id " +
            "WHERE r.projectId = :projectId ORDER BY o.createdAtMillis ASC"
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
