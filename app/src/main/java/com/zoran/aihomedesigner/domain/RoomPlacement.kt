package com.zoran.aihomedesigner.domain

import kotlin.math.abs

/** Tačka u koordinatnom sistemu projekta, u metrima. */
data class PlanPoint(val xM: Double, val yM: Double)

object RoomPlacement {

    /** Razmak između soba pri automatskom postavljanju nove sobe, u metrima. */
    const val GAP_M = 1.0

    /** Pri prevlačenju prstom položaj se zaokružuje na ovaj korak (5 cm). */
    const val GRID_M = 0.05

    /** Najveća dozvoljena koordinata po apsolutnoj vrednosti (1 km), da se uhvate očigledne greške. */
    const val MAX_COORDINATE_M = 1000.0

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

    /**
     * Daje položaj svim sobama koje ga nemaju (npr. sobe napravljene pre uvođenja plana),
     * redom po vremenu pravljenja. Vraća samo sobe koje su dobile novi položaj.
     */
    fun autoPlace(rooms: List<Room>): List<Room> {
        val current = rooms.toMutableList()
        val changed = mutableListOf<Room>()
        rooms.filter { !it.isPlaced }.sortedBy { it.createdAtMillis }.forEach { room ->
            val position = nextPosition(current)
            val placed = room.copy(xM = position.xM, yM = position.yM)
            current[current.indexOfFirst { it.id == room.id }] = placed
            changed += placed
        }
        return changed
    }

    /** Zaokružuje na korak od 5 cm (za prevlačenje prstom). */
    fun snapToGrid(value: Double): Double = Math.round(value * 20.0) / 20.0

    /** Zaokružuje na milimetar, da ponovljena pomeranja ne gomilaju sitne greške u zapisu. */
    fun roundToMm(value: Double): Double = Math.round(value * 1000.0) / 1000.0

    fun isValidCoordinate(value: Double): Boolean =
        value.isFinite() && abs(value) <= MAX_COORDINATE_M

    fun clampCoordinate(value: Double): Double =
        value.coerceIn(-MAX_COORDINATE_M, MAX_COORDINATE_M)
}
