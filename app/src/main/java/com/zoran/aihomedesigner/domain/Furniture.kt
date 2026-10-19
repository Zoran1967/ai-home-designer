package com.zoran.aihomedesigner.domain

import java.util.UUID

enum class FurnitureCategory { BED, SOFA, TABLE, CHAIR, WARDROBE, DESK, SHELF, APPLIANCE, OTHER }

/**
 * Komad nameštaja u jednoj sobi.
 *
 * Mere su u metrima. lengthM i widthM su dimenzije predmeta kad nije okrenut (rotationDeg = 0),
 * heightM je visina.
 *
 * Položaj (xM, yM) je gornji levi ugao pravougaonika koji predmet zauzima na podu, meren od gornjeg
 * levog ugla unutrašnjosti sobe (x udesno, y nadole). Položaj je u odnosu na sobu, pa se nameštaj
 * pomera zajedno sa sobom na planu.
 *
 * rotationDeg je 0, 90, 180 ili 270. Za 90 i 270 se dužina i širina menjaju mesta na podu.
 */
data class Furniture(
    val id: String,
    val roomId: String,
    val name: String,
    val category: FurnitureCategory,
    val lengthM: Double,
    val widthM: Double,
    val heightM: Double,
    val xM: Double,
    val yM: Double,
    val rotationDeg: Int,
    val dimensionSource: DimensionSource,
    val createdAtMillis: Long
) {
    /** Koliko predmet zauzima duž ose x sobe, posle okretanja. */
    val footprintLengthM: Double get() = if (rotationDeg % 180 == 0) lengthM else widthM

    /** Koliko predmet zauzima duž ose y sobe, posle okretanja. */
    val footprintWidthM: Double get() = if (rotationDeg % 180 == 0) widthM else lengthM

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
