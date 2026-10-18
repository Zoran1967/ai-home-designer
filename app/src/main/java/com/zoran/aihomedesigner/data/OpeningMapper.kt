package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.DoorSwing
import com.zoran.aihomedesigner.domain.HingeSide
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.OpeningType
import com.zoran.aihomedesigner.domain.WallSide

fun Opening.toEntity(): OpeningEntity = OpeningEntity(
    id = id,
    roomId = roomId,
    side = side.name,
    type = type.name,
    offsetM = offsetM,
    widthM = widthM,
    heightM = heightM,
    sillHeightM = sillHeightM,
    swing = swing?.name,
    hinge = hinge?.name,
    dimensionSource = dimensionSource.name,
    createdAtMillis = createdAtMillis
)

/** Vraća null ako je neka vrednost u bazi nepoznata, da ne prikažemo pogrešne podatke. */
fun OpeningEntity.toDomainOrNull(): Opening? {
    val wallSide = WallSide.values().firstOrNull { it.name == side } ?: return null
    val openingType = OpeningType.values().firstOrNull { it.name == type } ?: return null
    val source = DimensionSource.values().firstOrNull { it.name == dimensionSource } ?: return null

    val swingValue = if (swing == null) null else {
        DoorSwing.values().firstOrNull { it.name == swing } ?: return null
    }
    val hingeValue = if (hinge == null) null else {
        HingeSide.values().firstOrNull { it.name == hinge } ?: return null
    }

    return Opening(
        id = id,
        roomId = roomId,
        side = wallSide,
        type = openingType,
        offsetM = offsetM,
        widthM = widthM,
        heightM = heightM,
        sillHeightM = sillHeightM,
        swing = swingValue,
        hinge = hingeValue,
        dimensionSource = source,
        createdAtMillis = createdAtMillis
    )
}
