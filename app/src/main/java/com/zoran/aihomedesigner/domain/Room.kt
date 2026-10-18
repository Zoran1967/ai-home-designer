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
 * Sve mere su u metrima. Dužina i širina su unutrašnje mere (od zida do zida).
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
    val createdAtMillis: Long
) {
    /** Površina poda, izračunata iz dužine i širine. */
    val areaM2: Double get() = lengthM * widthM

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
