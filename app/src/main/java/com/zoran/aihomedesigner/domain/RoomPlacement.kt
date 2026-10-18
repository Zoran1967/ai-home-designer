package com.zoran.aihomedesigner.domain

/** Tačka u koordinatnom sistemu projekta, u metrima. */
data class PlanPoint(val xM: Double, val yM: Double)

object RoomPlacement {

    /** Razmak između soba pri automatskom postavljanju nove sobe, u metrima. */
    const val GAP_M = 1.0

    /**
     * Početni položaj za novu sobu: desno od najdesnije sobe koja već ima položaj,
     * u visini najgornje sobe. Ako nijedna soba nema položaj, to je (0, 0).
     *
     * Ovo je samo početni raspored. Korisnik sobu kasnije sam postavlja na pravo mesto,
     * pa ovaj položaj ne predstavlja izmerenu vrednost.
     */
    fun nextPosition(existing: List<Room>): PlanPoint {
        val placed = existing.filter { it.isPlaced }
        if (placed.isEmpty()) return PlanPoint(0.0, 0.0)

        val rightEdge = placed.maxOf { it.xM!! + it.lengthM }
        val topEdge = placed.minOf { it.yM!! }
        return PlanPoint(rightEdge + GAP_M, topEdge)
    }
}
