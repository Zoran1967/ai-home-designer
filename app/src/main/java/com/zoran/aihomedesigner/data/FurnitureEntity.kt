package com.zoran.aihomedesigner.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureCategory

/**
 * Red u tabeli "furniture". Brisanjem sobe brišu se i njeni predmeti.
 * Mere i položaj su u metrima (položaj od gornjeg levog ugla sobe). Enum vrednosti se čuvaju po imenu.
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
    val rotationDeg: Int,
    val dimensionSource: String,
    val createdAtMillis: Long
)

fun Furniture.toEntity(): FurnitureEntity = FurnitureEntity(
    id = id,
    roomId = roomId,
    name = name,
    category = category.name,
    lengthM = lengthM,
    widthM = widthM,
    heightM = heightM,
    xM = xM,
    yM = yM,
    rotationDeg = rotationDeg,
    dimensionSource = dimensionSource.name,
    createdAtMillis = createdAtMillis
)

/** Vraća null ako je neka vrednost u bazi nepoznata, da ne prikažemo pogrešne podatke. */
fun FurnitureEntity.toDomainOrNull(): Furniture? {
    val furnitureCategory = FurnitureCategory.values().firstOrNull { it.name == category } ?: return null
    val source = DimensionSource.values().firstOrNull { it.name == dimensionSource } ?: return null
    return Furniture(
        id = id,
        roomId = roomId,
        name = name,
        category = furnitureCategory,
        lengthM = lengthM,
        widthM = widthM,
        heightM = heightM,
        xM = xM,
        yM = yM,
        rotationDeg = rotationDeg,
        dimensionSource = source,
        createdAtMillis = createdAtMillis
    )
}
