package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.DoorSwing
import com.zoran.aihomedesigner.domain.HingeEnd
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.OpeningType
import com.zoran.aihomedesigner.domain.WallSide

fun Opening.toEntity(): OpeningEntity = OpeningEntity(
    id = id,
    roomId = roomId,
    type = type.name,
    side = side.name,
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
    val openingType = OpeningType.values().firstOrNull { it.name == type } ?: return null
    val wallSide = WallSide.values().firstOrNull { it.name == side } ?: return null
    val source = DimensionSource.values().firstOrNull { it.name == dimensionSource } ?: return null

    val doorSwing = if (swing == null) null else {
        DoorSwing.values().firstOrNull { it.name == swing } ?: return null
    }
    val hingeEnd = if (hinge == null) null else {
        HingeEnd.values().firstOrNull { it.name == hinge } ?: return null
    }

    return Opening(
        id = id,
        roomId = roomId,
        type = openingType,
        side = wallSide,
        offsetM = offsetM,
        widthM = widthM,
        heightM = heightM,
        sillHeightM = sillHeightM,
        swing = doorSwing,
        hinge = hingeEnd,
        dimensionSource = source,
        createdAtMillis = createdAtMillis
    )
}
