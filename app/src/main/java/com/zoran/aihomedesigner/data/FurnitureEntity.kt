package com.zoran.aihomedesigner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Red u tabeli "furniture". Brisanjem sobe brišu se i njeni predmeti.
 * Mere i položaj su u metrima (položaj od gornjeg levog ugla unutrašnjosti sobe).
 */
@Entity(
    tableName = "furniture",
    foreignKeys = [
        ForeignKey(
            entity = RoomEntity::class,
            parentColumns = ["id"],
            childColumns = ["roomId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("roomId")]
)
data class FurnitureEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val name: String,
    val category: String,
    val lengthM: Double,
    val widthM: Double,
    val heightM: Double,
    val xM: Double,
    val yM: Double,
    val quarterTurns: Int,
    val dimensionSource: String,
    val createdAtMillis: Long
)
