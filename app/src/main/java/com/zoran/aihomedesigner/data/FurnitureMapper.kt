package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureCategory

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
    quarterTurns = quarterTurns,
    dimensionSource = dimensionSource.name,
    createdAtMillis = createdAtMillis
)

/** Vraća null ako je neka vrednost u bazi nepoznata ili neispravna, da ne prikažemo pogrešne podatke. */
fun FurnitureEntity.toDomainOrNull(): Furniture? {
    val furnitureCategory = FurnitureCategory.values().firstOrNull { it.name == category } ?: return null
    val source = DimensionSource.values().firstOrNull { it.name == dimensionSource } ?: return null
    if (quarterTurns !in 0..3) return null
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
        quarterTurns = quarterTurns,
        dimensionSource = source,
        createdAtMillis = createdAtMillis
    )
}
