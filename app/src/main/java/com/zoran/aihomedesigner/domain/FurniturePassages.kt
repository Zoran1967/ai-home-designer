package com.zoran.aihomedesigner.domain

import kotlin.math.max
import kotlin.math.min

/**
 * Uzak prolaz između nameštaja: razmak koji liči na prolaz, ali je uži od preporučenog.
 * Ako je [otherFurnitureId] null, razmak je između predmeta i zida sobe [wall].
 */
data class NarrowPassage(
    val furnitureId: String,
    val otherFurnitureId: String?,
    val wall: WallSide?,
    val gapM: Double
)

/**
 * Traži uske prolaze u jednoj sobi. Ovo je PREPORUKA, a ne greška: predmet blizu zida ili drugog predmeta
 * može da bude namerno tako postavljen. Proverava samo razmake koji su duži od [MIN_FACING_M] duž
 * susedne ivice, a ne proverava da li se kroz razmak stvarno prolazi.
 */
object FurniturePassages {

    /** Preporučena najmanja širina prolaza (60 cm). Razmak jednak ovoj vrednosti je dovoljan. */
    const val MIN_PASSAGE_M = 0.6

    /** Razmaci kraći od ovoga (20 cm) ne liče na prolaz, pa se ne prijavljuju. */
    const val MIN_GAP_M = 0.2

    /** Najmanja dužina na kojoj se dve ivice "gledaju" da bi razmak imao smisla kao prolaz (50 cm). */
    const val MIN_FACING_M = 0.5

    private const val EPS = RoomGeometry.EPSILON_M

    private fun isNarrow(gap: Double): Boolean = gap >= MIN_GAP_M - EPS && gap < MIN_PASSAGE_M - EPS

    fun find(room: Room, furniture: List<Furniture>): List<NarrowPassage> {
        val rects = furniture.map { FurnitureGeometry.rect(it) }
        val result = mutableListOf<NarrowPassage>()

        for (i in furniture.indices) {
            for (j in i + 1 until furniture.size) {
                val a = rects[i]
                val b = rects[j]
                if (FurnitureGeometry.intersects(a, b)) continue // preklapanje prijavljuje druga provera

                val facingY = min(a.y1, b.y1) - max(a.y0, b.y0)
                val gapX = max(b.x0 - a.x1, a.x0 - b.x1)
                if (facingY >= MIN_FACING_M - EPS && isNarrow(gapX)) {
                    result += NarrowPassage(furniture[i].id, furniture[j].id, null, gapX)
                    continue
                }

                val facingX = min(a.x1, b.x1) - max(a.x0, b.x0)
                val gapY = max(b.y0 - a.y1, a.y0 - b.y1)
                if (facingX >= MIN_FACING_M - EPS && isNarrow(gapY)) {
                    result += NarrowPassage(furniture[i].id, furniture[j].id, null, gapY)
                }
            }
        }

        for (i in furniture.indices) {
            val r = rects[i]
            val id = furniture[i].id
            val alongX = r.x1 - r.x0
            val alongY = r.y1 - r.y0
            if (alongY >= MIN_FACING_M - EPS) {
                if (isNarrow(r.x0)) result += NarrowPassage(id, null, WallSide.WEST, r.x0)
                if (isNarrow(room.lengthM - r.x1)) result += NarrowPassage(id, null, WallSide.EAST, room.lengthM - r.x1)
            }
            if (alongX >= MIN_FACING_M - EPS) {
                if (isNarrow(r.y0)) result += NarrowPassage(id, null, WallSide.NORTH, r.y0)
                if (isNarrow(room.widthM - r.y1)) result += NarrowPassage(id, null, WallSide.SOUTH, room.widthM - r.y1)
            }
        }
        return result
    }
}
