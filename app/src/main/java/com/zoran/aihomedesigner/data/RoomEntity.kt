package com.zoran.aihomedesigner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Red u tabeli "rooms". Brisanjem projekta brišu se i njegove sobe. Mere i položaj su u metrima.
 * originXM i originYM su gornji levi ugao sobe na planu (x udesno, y nadole).
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
    @ColumnInfo(defaultValue = "0") val originXM: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val originYM: Double = 0.0
)
