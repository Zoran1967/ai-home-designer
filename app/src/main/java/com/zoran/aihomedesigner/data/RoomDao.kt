package com.zoran.aihomedesigner.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {

    @Query("SELECT * FROM rooms WHERE projectId = :projectId ORDER BY createdAtMillis ASC")
    fun observeByProject(projectId: String): Flow<List<RoomEntity>>

    @Query("SELECT name FROM rooms WHERE projectId = :projectId")
    suspend fun getNamesByProject(projectId: String): List<String>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE id = :id")
    suspend fun deleteById(id: String)
}
