package com.zoran.aihomedesigner.domain

import java.util.UUID

/** Zid sobe, po stranama sveta na planu (gore je sever). Vidi docs/koordinatni-sistem.md. */
enum class WallSide { NORTH, EAST, SOUTH, WEST }

enum class OpeningType { DOOR, WINDOW }

/** Na koju stranu se vrata otvaraju u odnosu na sobu kojoj pripadaju. */
enum class DoorSwing { INTO_ROOM, OUT_OF_ROOM }

/** Na kom kraju otvora su šarke, gledano duž zida od njegovog početka (vidi docs/koordinatni-sistem.md). */
enum class HingeSide { START, END }

/**
 * Vrata ili prozor na zidu sobe. Sve mere su u metrima.
 *
 * offsetM: rastojanje od početka zida do bližeg kraja otvora.
 * Početak zida je zapadni kraj za severni i južni zid, a severni kraj za istočni i zapadni zid.
 * widthM: širina otvora duž zida. heightM: visina otvora.
 * sillHeightM: visina donje ivice otvora od poda (za vrata je 0).
 * swing i hinge se odnose samo na vrata, a za prozore su null.
 */
data class Opening(
    val id: String,
    val roomId: String,
    val side: WallSide,
    val type: OpeningType,
    val offsetM: Double,
    val widthM: Double,
    val heightM: Double,
    val sillHeightM: Double,
    val swing: DoorSwing?,
    val hinge: HingeSide?,
    val dimensionSource: DimensionSource,
    val createdAtMillis: Long
) {
    /** Rastojanje od početka zida do drugog kraja otvora. */
    val endM: Double get() = offsetM + widthM

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

/** Dužina zida sobe: severni i južni zid su dugi koliko i dužina sobe, istočni i zapadni koliko i širina. */
fun Room.wallLengthM(side: WallSide): Double = when (side) {
    WallSide.NORTH, WallSide.SOUTH -> lengthM
    WallSide.EAST, WallSide.WEST -> widthM
}
