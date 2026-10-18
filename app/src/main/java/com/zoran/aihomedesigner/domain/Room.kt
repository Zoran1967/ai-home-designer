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
 * Soba u projektu. Sada samo pravougaona.
 *
 * Sve mere su u metrima. Dužina (lengthM) i širina (widthM) su unutrašnje mere (od zida do zida).
 * Debljina zida je opciona: null znači "nije uneto".
 *
 * Položaj na planu (xM, yM): gornji levi ugao unutrašnjosti sobe, u metrima, u koordinatnom
 * sistemu projekta (x ide udesno, y ide nadole, vidi docs/koordinatni-sistem.md).
 * Soba zauzima pravougaonik od (xM, yM) do (xM + lengthM, yM + widthM).
 * null znači da soba još nema položaj na planu. To NIJE isto što i položaj (0, 0).
 * Položaj sobe je raspored na planu i nije izmerena vrednost.
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
    val xM: Double? = null,
    val yM: Double? = null
) {
    /** Površina poda, izračunata iz dužine i širine. */
    val areaM2: Double get() = lengthM * widthM

    val isPlaced: Boolean get() = xM != null && yM != null

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
