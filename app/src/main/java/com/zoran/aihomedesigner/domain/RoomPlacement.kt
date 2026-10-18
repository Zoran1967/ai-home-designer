package com.zoran.aihomedesigner.domain

/** Početni raspored novih soba na planu. Ovo nije merenje, korisnik ga kasnije menja. */
object RoomPlacement {

    /** Razmak između soba pri početnom rasporedu, u metrima. */
    const val GAP_M = 0.5

    data class Origin(val xM: Double, val yM: Double)

    /**
     * Položaj nove sobe: desno od svih postojećih soba, na gornjoj liniji plana (y = 0).
     * Tako se nova soba ne preklapa ni sa jednom postojećom.
     */
    fun nextOrigin(existing: List<Room>): Origin {
        if (existing.isEmpty()) return Origin(0.0, 0.0)
        val rightmost = existing.maxOf { it.rightM }
        return Origin(rightmost + GAP_M, 0.0)
    }
}
