package com.zoran.aihomedesigner.domain

import java.util.UUID

/**
 * Odakle potiče mera. Procena nikada ne sme da se prikaže kao proverena mera.
 * Za sada se koristi samo USER_CONFIRMED (korisnik sam upisao meru).
 */
enum class DimensionSource {
    USER_CONFIRMED,
    IMPORTED_CALIBRATED,
    DETECTED_UNCONFIRMED,
    ESTIMATED
}

/**
 * Soba u projektu. Sada samo pravougaona, bez rotacije.
 *
 * KOORDINATNI SISTEM PLANA (isti za ceo projekat):
 * - jedinica je metar,
 * - x raste udesno, y raste nadole (kao na ekranu telefona),
 * - tačka (0, 0) je proizvoljna početna tačka plana, a ne neka stvarna tačka u zgradi,
 * - (originXM, originYM) je gornji levi ugao sobe,
 * - dužina sobe ide duž x ose, a širina duž y ose.
 *
 * Mere sobe (dužina, širina, visina, debljina zida) su unutrašnje i u metrima.
 * Položaj sobe na planu je raspored koji korisnik slaže, a ne izmerena vrednost.
 * Debljina zida je opciona: null znači "nije uneto".
 */
data class Room(
    val id: String,
    val projectId: String,
    val name: String,
    val lengthM: Double,
    val widthM: Double,
    val heightM: Double,
    val wallThicknessM: Double?,
    val dimensionSource: DimensionSource,
    val createdAtMillis: Long,
    val originXM: Double = 0.0,
    val originYM: Double = 0.0
) {
    /** Površina poda, izračunata iz dužine i širine. */
    val areaM2: Double get() = lengthM * widthM

    /** Desna ivica sobe na planu. */
    val rightM: Double get() = originXM + lengthM

    /** Donja ivica sobe na planu. */
    val bottomM: Double get() = originYM + widthM

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
