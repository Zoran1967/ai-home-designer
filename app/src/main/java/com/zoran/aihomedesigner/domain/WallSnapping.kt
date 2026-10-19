package com.zoran.aihomedesigner.domain

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Priljubljivanje sobe uz susednu sobu tako da razmak među njima bude tačno debljina zida.
 * Soba se pomera samo po jednoj osi (druga koordinata ostaje ista).
 */
object WallSnapping {

    /** Najveće rastojanje za koje se soba sama pomera (3 m). */
    const val MAX_SNAP_DISTANCE_M = 3.0

    sealed interface Result {
        /** Soba se pomera na [position]; zid prema sobi [neighborId] je debeo [thicknessM]. */
        data class Moved(
            val position: PlanPoint,
            val neighborId: String,
            val thicknessM: Double
        ) : Result

        /** Nema susedne sobe koja je dovoljno blizu, okrenuta stranicom ka ovoj sobi. */
        data object NoNeighborNearby : Result

        /** Susedna soba postoji, ali debljina zida nije uneta ni za jednu od dve sobe. */
        data object NeedsThickness : Result
    }

    /**
     * Debljina zida je veća od dve unete debljine (za ovu i za susednu sobu).
     * Ako nijedna nije uneta, debljina se ne izmišlja, već se traži od korisnika.
     */
    private fun thicknessFor(room: Room, other: Room): Double? =
        listOfNotNull(room.wallThicknessM, other.wallThicknessM).maxOrNull()

    fun snap(rooms: List<Room>, roomId: String): Result {
        val room = rooms.firstOrNull { it.id == roomId && it.isPlaced } ?: return Result.NoNeighborNearby
        val others = rooms.filter { it.id != roomId && it.isPlaced }

        var best: Result.Moved? = null
        var bestDistance = Double.MAX_VALUE
        var neighborNeedsThickness = false

        for (other in others) {
            val thickness = thicknessFor(room, other)
            val t = thickness ?: 0.0
            for (candidate in candidates(room, other, t)) {
                val moved = room.copy(xM = candidate.xM, yM = candidate.yM)
                val distance = hypot(candidate.xM - room.xM!!, candidate.yM - room.yM!!)
                if (distance > MAX_SNAP_DISTANCE_M) continue
                if (!facesEachOther(moved, other)) continue
                if (others.any { RoomGeometry.overlaps(moved, it) }) continue

                if (thickness == null) {
                    neighborNeedsThickness = true
                } else if (distance < bestDistance) {
                    bestDistance = distance
                    best = Result.Moved(
                        position = PlanPoint(
                            RoomPlacement.roundToMm(candidate.xM),
                            RoomPlacement.roundToMm(candidate.yM)
                        ),
                        neighborId = other.id,
                        thicknessM = thickness
                    )
                }
            }
        }

        return best ?: if (neighborNeedsThickness) Result.NeedsThickness else Result.NoNeighborNearby
    }

    /**
     * Četiri moguća položaja uz susednu sobu: desno, levo, ispod i iznad nje.
     * Uzima se u obzir samo strana na kojoj se soba već nalazi (po središtima soba),
     * da soba ne "preskoči" susednu.
     */
    private fun candidates(room: Room, other: Room, t: Double): List<PlanPoint> {
        val roomCx = room.xM!! + room.lengthM / 2
        val roomCy = room.yM!! + room.widthM / 2
        val otherCx = other.xM!! + other.lengthM / 2
        val otherCy = other.yM!! + other.widthM / 2

        val result = mutableListOf<PlanPoint>()
        if (roomCx >= otherCx) result += PlanPoint(other.xM + other.lengthM + t, room.yM)
        if (roomCx <= otherCx) result += PlanPoint(other.xM - room.lengthM - t, room.yM)
        if (roomCy >= otherCy) result += PlanPoint(room.xM, other.yM!! + other.widthM + t)
        if (roomCy <= otherCy) result += PlanPoint(room.xM, other.yM - room.widthM - t)
        return result
    }

    /** Da li se dve sobe, posle pomeranja, dodiruju stranicom bar [SharedWalls.MIN_SHARED_LENGTH_M]. */
    private fun facesEachOther(a: Room, b: Room): Boolean {
        val overlapY = min(a.yM!! + a.widthM, b.yM!! + b.widthM) - max(a.yM, b.yM!!)
        val overlapX = min(a.xM!! + a.lengthM, b.xM!! + b.lengthM) - max(a.xM, b.xM!!)
        return overlapY >= SharedWalls.MIN_SHARED_LENGTH_M || overlapX >= SharedWalls.MIN_SHARED_LENGTH_M
    }
}
