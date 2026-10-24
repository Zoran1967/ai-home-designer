package com.zoran.aihomedesigner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StairsDao {

    @Query("SELECT * FROM stairs WHERE projectId = :projectId ORDER BY createdAtMillis ASC")
    fun observeByProject(projectId: String): Flow<List<StairsEntity>>

    @Query("SELECT * FROM stairs WHERE projectId = :projectId")
    suspend fun getByProject(projectId: String): List<StairsEntity>

    @Query("SELECT * FROM stairs WHERE id = :id")
    suspend fun getById(id: String): StairsEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(stairs: StairsEntity)

    @Update
    suspend fun update(stairs: StairsEntity)

    /** Menja samo položaj na planu, ostala polja ostaju netaknuta. */
    @Query("UPDATE stairs SET xM = :xM, yM = :yM WHERE id = :id")
    suspend fun updatePosition(id: String, xM: Double, yM: Double)

    @Query("DELETE FROM stairs WHERE id = :id")
    suspend fun deleteById(id: String)
}
