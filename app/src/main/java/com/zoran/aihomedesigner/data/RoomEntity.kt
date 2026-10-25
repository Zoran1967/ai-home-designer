package com.zoran.aihomedesigner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Red u tabeli "rooms". Brisanjem projekta brišu se i njegove sobe. Mere su u metrima.
 * xM i yM su položaj na planu; null znači da soba još nema položaj.
 * floorLevel je sprat (0 = prizemlje).
 */
@Entity(
    tableName = "rooms",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class RoomEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val lengthM: Double,
    val widthM: Double,
    val heightM: Double,
    val wallThicknessM: Double?,
    val dimensionSource: String,
    val createdAtMillis: Long,
    val xM: Double? = null,
    val yM: Double? = null,
    @ColumnInfo(defaultValue = "0") val floorLevel: Int = 0,
    @ColumnInfo(defaultValue = "0") val positionLocked: Boolean = false,
    val glueGroupId: String? = null
)
