package com.zoran.aihomedesigner.domain

import java.util.UUID

/**
 * Prave stepenice koje spajaju sprat [lowerLevel] sa spratom iznad njega ([upperLevel] = lowerLevel + 1).
 *
 * Mere su u metrima. [lengthM] je dužina kraka u pravcu penjanja (horizontalno, bez visine), a [widthM] je širina.
 * Visina (uspon) se ne čuva: ona proizlazi iz visine sprata i debljine međuspratne ploče.
 *
 * Položaj (xM, yM) je gornji levi ugao pravougaonika koji stepenice zauzimaju na planu, u koordinatama plana
 * (x udesno, y nadole, u metrima), kao kod soba.
 *
 * [directionDeg] je pravac u kojem se ide NAGORE: 0 = ka istoku (+x), 90 = ka jugu (+y), 180 = ka zapadu (-x),
 * 270 = ka severu (-y). Za 90 i 270 dužina i širina zamene mesta na planu.
 */
data class Stairs(
    val id: String,
    val projectId: String,
    val lowerLevel: Int,
    val xM: Double,
    val yM: Double,
    val lengthM: Double,
    val widthM: Double,
    val directionDeg: Int,
    val dimensionSource: DimensionSource,
    val createdAtMillis: Long
) {
    /** Sprat do kojeg stepenice vode. */
    val upperLevel: Int get() = lowerLevel + 1

    /** Koliko stepenice zauzimaju duž ose x plana. */
    val footprintLengthM: Double get() = if (directionDeg % 180 == 0) lengthM else widthM

    /** Koliko stepenice zauzimaju duž ose y plana. */
    val footprintWidthM: Double get() = if (directionDeg % 180 == 0) widthM else lengthM

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

/** Granice služe samo da uhvate očigledne greške u unosu. Ne garantuju tačnost mere. */
object StairsLimits {
    const val MIN_LENGTH_M = 1.0
    const val MAX_LENGTH_M = 8.0
    const val MIN_WIDTH_M = 0.5
    const val MAX_WIDTH_M = 3.0
}
