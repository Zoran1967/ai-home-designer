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
 *
 * Sprat (floorLevel): 0 je prizemlje, 1 je prvi sprat, -1 je podrum. Sobe na različitim spratovima
 * ne proveravaju se međusobno (preklapanje, zidovi), a svaki sprat ima svoj plan.
 *
 * Zaključavanje (positionLocked): zaključana soba se ne pomera na planu (prstom, strelicama ni
 * priljubljivanjem), da se ne pomeri slučajnim dodirom.
 *
 * Lepljenje (glueGroupId): sobe istog sprata sa istim glueGroupId (koji nije null) su zalepljene i
 * pomeraju se zajedno, pa se pri pomeranju ne razilaze. Grupa sa jednom sobom nije zalepljena.
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
    val yM: Double? = null,
    val floorLevel: Int = 0,
    val positionLocked: Boolean = false,
    val glueGroupId: String? = null
) {
    /** Površina poda, izračunata iz dužine i širine. */
    val areaM2: Double get() = lengthM * widthM

    val isPlaced: Boolean get() = xM != null && yM != null

    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}

/** Pomoćne funkcije za spratove jednog projekta. */
object Floors {
    const val MIN_LEVEL = -2 // dva podruma
    const val MAX_LEVEL = 9

    fun isValidLevel(level: Int): Boolean = level in MIN_LEVEL..MAX_LEVEL

    /** Spratovi koji postoje: nivoi svih soba, uvek uključujući prizemlje (0), od najnižeg ka najvišem. */
    fun levels(rooms: List<Room>): List<Int> = (rooms.map { it.floorLevel } + 0).distinct().sorted()

    /** Prvi viši sprat od postojećih, ili null ako je dostignut najviši dozvoljeni. */
    fun nextAbove(rooms: List<Room>): Int? = (levels(rooms).max() + 1).takeIf { isValidLevel(it) }

    /** Prvi niži sprat od postojećih, ili null ako je dostignut najniži dozvoljeni. */
    fun nextBelow(rooms: List<Room>): Int? = (levels(rooms).min() - 1).takeIf { isValidLevel(it) }
}
