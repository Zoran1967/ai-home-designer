package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Room

fun Room.toEntity(): RoomEntity = RoomEntity(
    id = id,
    projectId = projectId,
    name = name,
    lengthM = lengthM,
    widthM = widthM,
    heightM = heightM,
    wallThicknessM = wallThicknessM,
    dimensionSource = dimensionSource.name,
    createdAtMillis = createdAtMillis,
    xM = xM,
    yM = yM,
    floorLevel = floorLevel,
    positionLocked = positionLocked,
    glueGroupId = glueGroupId
)

/** Vraća null ako je izvor mere u bazi nepoznat, da ne prikažemo pogrešne podatke. */
fun RoomEntity.toDomainOrNull(): Room? {
    val source = DimensionSource.values().firstOrNull { it.name == dimensionSource }
        ?: return null
    return Room(
        id = id,
        projectId = projectId,
        name = name,
        lengthM = lengthM,
        widthM = widthM,
        heightM = heightM,
        wallThicknessM = wallThicknessM,
        dimensionSource = source,
        createdAtMillis = createdAtMillis,
        xM = xM,
        yM = yM,
        floorLevel = floorLevel,
        positionLocked = positionLocked,
        glueGroupId = glueGroupId
    )
}
