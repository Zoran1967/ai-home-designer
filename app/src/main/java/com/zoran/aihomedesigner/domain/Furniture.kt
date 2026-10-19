package com.zoran.aihomedesigner.domain

import java.util.UUID

enum class FurnitureCategory {
    BED,
    SOFA,
    TABLE,
    CHAIR,
    WARDROBE,
    DESK,
    SHELF,
    KITCHEN_UNIT,
    APPLIANCE,
    OTHER
}

/**
 * Komad nameštaja u jednoj sobi. Sve mere su u metrima.
 *
 * lengthM, widthM i heightM su prave mere predmeta (dužina, širina, visina).
 * Pri quarterTurns = 0 dužina ide duž ose x sobe, a širina duž ose y.
 *
 * Položaj (xM, yM) je gornji levi ugao tlocrta predmeta, meren od gornjeg levog ugla
 * unutrašnjosti sobe. Meri se od sobe, a ne od celog plana, da se nameštaj pomera zajedno sa sobom.
 *
 * quarterTurns (0 do 3) je broj okretanja za po 90 stepeni u smeru kazaljke na satu.
 * Pri neparnom broju okretanja dužina i širina zamenjuju mesta u tlocrtu.
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
    val quarterTurns: Int,
    val dimensionSource: DimensionSource,
    val createdAtMillis: Long
) {
    /** Dimenzija tlocrta duž ose x. */
    val footprintLengthM: Double get() = if (quarterTurns % 2 == 0) lengthM else widthM

    /** Dimenzija tlocrta duž ose y. */
    val footprintWidthM: Double get() = if (quarterTurns % 2 == 0) widthM else lengthM

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
