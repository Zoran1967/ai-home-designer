package com.zoran.aihomedesigner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FurnitureDao {

    @Query("SELECT * FROM furniture WHERE roomId = :roomId ORDER BY createdAtMillis ASC")
    fun observeByRoom(roomId: String): Flow<List<FurnitureEntity>>

    /** Sav nameštaj svih soba jednog projekta (za crtanje plana). */
    @Query(
        "SELECT f.* FROM furniture f INNER JOIN rooms r ON f.roomId = r.id " +
            "WHERE r.projectId = :projectId ORDER BY f.createdAtMillis ASC"
    )
    fun observeByProject(projectId: String): Flow<List<FurnitureEntity>>

    @Query("SELECT * FROM furniture WHERE roomId = :roomId")
    suspend fun getByRoom(roomId: String): List<FurnitureEntity>

    @Query("SELECT * FROM furniture WHERE id = :id")
    suspend fun getById(id: String): FurnitureEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(furniture: FurnitureEntity)

    @Update
    suspend fun update(furniture: FurnitureEntity)

    /** Menja samo položaj u sobi, ostala polja ostaju netaknuta. */
    @Query("UPDATE furniture SET xM = :xM, yM = :yM WHERE id = :id")
    suspend fun updatePosition(id: String, xM: Double, yM: Double)

    @Query("UPDATE furniture SET quarterTurns = :quarterTurns WHERE id = :id")
    suspend fun updateQuarterTurns(id: String, quarterTurns: Int)

    @Query("DELETE FROM furniture WHERE id = :id")
    suspend fun deleteById(id: String)
}
