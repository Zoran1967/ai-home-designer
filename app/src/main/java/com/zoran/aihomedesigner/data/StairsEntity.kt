package com.zoran.aihomedesigner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Stairs

/**
 * Red u tabeli "stairs". Brisanjem projekta brišu se i njegove stepenice.
 * Mere i položaj su u metrima (koordinate plana). lowerLevel je donji sprat, a stepenice vode na sprat iznad.
 * directionDeg je pravac penjanja (0, 90, 180 ili 270). Enum vrednosti se čuvaju po imenu.
 */
@Entity(
    tableName = "stairs",
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
data class StairsEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val lowerLevel: Int,
    val xM: Double,
    val yM: Double,
    val lengthM: Double,
    val widthM: Double,
    val directionDeg: Int,
    val dimensionSource: String,
    val createdAtMillis: Long
)

fun Stairs.toEntity(): StairsEntity = StairsEntity(
    id = id,
    projectId = projectId,
    lowerLevel = lowerLevel,
    xM = xM,
    yM = yM,
    lengthM = lengthM,
    widthM = widthM,
    directionDeg = directionDeg,
    dimensionSource = dimensionSource.name,
    createdAtMillis = createdAtMillis
)

/** Vraća null ako je neka vrednost u bazi nepoznata, da ne prikažemo pogrešne podatke. */
fun StairsEntity.toDomainOrNull(): Stairs? {
    val source = DimensionSource.values().firstOrNull { it.name == dimensionSource } ?: return null
    if (directionDeg !in listOf(0, 90, 180, 270)) return null
    return Stairs(
        id = id,
        projectId = projectId,
        lowerLevel = lowerLevel,
        xM = xM,
        yM = yM,
        lengthM = lengthM,
        widthM = widthM,
        directionDeg = directionDeg,
        dimensionSource = source,
        createdAtMillis = createdAtMillis
    )
}
