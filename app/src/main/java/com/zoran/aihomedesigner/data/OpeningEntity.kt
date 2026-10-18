package com.zoran.aihomedesigner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Red u tabeli "openings" (vrata i prozori). Brisanjem sobe brišu se i njeni otvori. Mere su u metrima. */
@Entity(
    tableName = "openings",
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
data class OpeningEntity(
    @PrimaryKey val id: String,
    val roomId: String,
    val side: String,
    val type: String,
    val offsetM: Double,
    val widthM: Double,
    val heightM: Double,
    val sillHeightM: Double,
    val swing: String?,
    val hinge: String?,
    val dimensionSource: String,
    val createdAtMillis: Long
)
