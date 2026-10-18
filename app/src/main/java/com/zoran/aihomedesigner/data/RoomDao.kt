package com.zoran.aihomedesigner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {

    @Query("SELECT * FROM rooms WHERE projectId = :projectId ORDER BY createdAtMillis ASC")
    fun observeByProject(projectId: String): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE projectId = :projectId")
    suspend fun getByProject(projectId: String): List<RoomEntity>

    @Query("SELECT * FROM rooms WHERE id = :id")
    suspend fun getById(id: String): RoomEntity?

    @Query("SELECT name FROM rooms WHERE projectId = :projectId")
    suspend fun getNamesByProject(projectId: String): List<String>

    /** Imena ostalih soba u projektu, bez sobe koja se menja (da se ne poredi sama sa sobom). */
    @Query("SELECT name FROM rooms WHERE projectId = :projectId AND id != :excludeId")
    suspend fun getNamesByProjectExcluding(projectId: String, excludeId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(room: RoomEntity)

    @Update
    suspend fun update(room: RoomEntity)

    /** Menja samo položaj na planu, ostala polja sobe ostaju netaknuta. */
    @Query("UPDATE rooms SET xM = :xM, yM = :yM WHERE id = :id")
    suspend fun updatePosition(id: String, xM: Double, yM: Double)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteById(id: String)
}
