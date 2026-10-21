package com.zoran.aihomedesigner.domain

import kotlin.math.max
import kotlin.math.min

/** Uzak razmak između dva predmeta nameštaja, u metrima. */
data class NarrowGap(val furnitureAId: String, val furnitureBId: String, val gapM: Double)

/**
 * Nalazi uske razmake između predmeta nameštaja u jednoj sobi (razmak je manji od [MIN_PASSAGE_M]).
 * To je napomena, a ne greška: aplikacija ne zna gde ljudi zaista prolaze, pa samo javlja gde bi prolaz,
 * ako ga ima, bio uži od 60 cm. Predmeti koji se dodiruju (razmak do 2 cm) i predmeti koji se preklapaju
 * (to je posebna greška) ne računaju se. Stolica uz sto ili radni sto se ne prijavljuje, jer je tu mali
 * razmak uobičajen.
 */
object FurniturePassages {

    /** Najmanja širina prolaza, u metrima (60 cm). */
    const val MIN_PASSAGE_M = 0.6

    /** Predmeti čiji je razmak do 2 cm smatraju se postavljenim jedan uz drugi. */
    const val TOUCHING_M = 0.02

    /** Predmeti se "gledaju" ako se preklapaju bar 30 cm duž stranice. */
    const val MIN_FACING_LENGTH_M = 0.3

    private const val EPS = RoomGeometry.EPSILON_M

    fun narrowGaps(furniture: List<Furniture>): List<NarrowGap> {
        val result = mutableListOf<NarrowGap>()
        for (i in furniture.indices) {
            for (j in i + 1 until furniture.size) {
                val a = furniture[i]
                val b = furniture[j]
                if (isSeatingPair(a, b)) continue
                val ra = FurnitureGeometry.rect(a)
                val rb = FurnitureGeometry.rect(b)
                if (FurnitureGeometry.intersects(ra, rb)) continue

                val gapX = max(rb.x0 - ra.x1, ra.x0 - rb.x1)
                val overlapY = min(ra.y1, rb.y1) - max(ra.y0, rb.y0)
                val gapY = max(rb.y0 - ra.y1, ra.y0 - rb.y1)
                val overlapX = min(ra.x1, rb.x1) - max(ra.x0, rb.x0)

                val gap = when {
                    isNarrow(gapX) && overlapY >= MIN_FACING_LENGTH_M - EPS -> gapX
                    isNarrow(gapY) && overlapX >= MIN_FACING_LENGTH_M - EPS -> gapY
                    else -> null
                }
                if (gap != null) result += NarrowGap(a.id, b.id, RoomPlacement.roundToMm(gap))
            }
        }
        return result
    }

    private fun isNarrow(gap: Double): Boolean = gap >= TOUCHING_M - EPS && gap < MIN_PASSAGE_M - EPS

    private fun isSeatingPair(a: Furniture, b: Furniture): Boolean {
        fun isTable(f: Furniture) = f.category == FurnitureCategory.TABLE || f.category == FurnitureCategory.DESK
        return (a.category == FurnitureCategory.CHAIR && isTable(b)) ||
            (b.category == FurnitureCategory.CHAIR && isTable(a))
    }
}
