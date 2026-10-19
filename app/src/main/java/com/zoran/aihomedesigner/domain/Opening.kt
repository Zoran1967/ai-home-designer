package com.zoran.aihomedesigner.domain

import java.util.UUID

enum class OpeningType { DOOR, WINDOW }

/** Strana sobe na kojoj je otvor, gledano na planu (sever je gore, istok desno). */
enum class WallSide { NORTH, EAST, SOUTH, WEST }

/** Način otvaranja vrata. SLIDING su klizna vrata. */
enum class DoorSwing { INTO_ROOM, OUT_OF_ROOM, SLIDING }

/** Na kom kraju otvora su šarke: START je kraj bliži početku zida, END je kraj bliži kraju zida. */
enum class HingeEnd { START, END }

/**
 * Vrata ili prozor u zidu jedne sobe.
 *
 * Svi razmaci i mere su u metrima.
 *
 * Položaj duž zida (offsetM) je rastojanje od početka zida do bliže ivice otvora.
 * Početak zida je zapadni ugao za severni i južni zid, a severni ugao za istočni i zapadni zid
 * (isto kao pravci na planu: x raste udesno, y raste nadole).
 *
 * heightM je visina otvora. sillHeightM je visina donje ivice od poda: 0 za vrata, a za prozor
 * ono što je korisnik uneo.
 * swing i hinge se koriste samo za vrata (hinge nije potreban za klizna vrata).
 */
data class Opening(
    val id: String,
    val roomId: String,
    val type: OpeningType,
    val side: WallSide,
    val offsetM: Double,
    val widthM: Double,
    val heightM: Double,
    val sillHeightM: Double,
    val swing: DoorSwing?,
    val hinge: HingeEnd?,
    val dimensionSource: DimensionSource,
    val createdAtMillis: Long
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

object WallGeometry {
    /** Dužina zida date strane sobe: dužina sobe za severni i južni, širina sobe za istočni i zapadni zid. */
    fun wallLengthM(room: Room, side: WallSide): Double = when (side) {
        WallSide.NORTH, WallSide.SOUTH -> room.lengthM
        WallSide.EAST, WallSide.WEST -> room.widthM
    }
}
