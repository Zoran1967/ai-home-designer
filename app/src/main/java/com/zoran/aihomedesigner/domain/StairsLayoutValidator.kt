package com.zoran.aihomedesigner.domain

sealed interface StairsIssue {
    /** Donji sprat ima sobe, ali nijedna ne obuhvata celu površinu koju stepenice zauzimaju. */
    data object NotInsideRoomBelow : StairsIssue

    /** Gornji sprat ima sobe, ali nijedna ne obuhvata celu površinu stepenica (otvor u podu bi bio van sobe). */
    data object NotInsideRoomAbove : StairsIssue

    /** Preklapa se sa drugim stepenicama koje kreću sa istog donjeg sprata. */
    data class OverlapsStairs(val otherStairsId: String) : StairsIssue
}

/**
 * Provera položaja stepenica. Proverava SAMO: da li je cela površina stepenica unutar jedne sobe donjeg sprata,
 * da li je unutar jedne sobe gornjeg sprata i da li se preklapa sa drugim stepenicama istog donjeg sprata.
 * Sprat bez soba (ili sa sobama bez položaja na planu) se ne proverava, jer nema sa čim da se uporedi.
 * Ne proverava prolaz kroz vrata, visinu iznad glave ni zidove između soba. Prazan spisak znači samo da
 * ove provere nisu našle problem.
 *
 * Stepenice koje se nadovezuju (jedne stižu na sprat sa kojeg druge kreću) ne smatraju se preklapanjem.
 */
object StairsLayoutValidator {

    fun validate(stairs: Stairs, allStairs: List<Stairs>, rooms: List<Room>): List<StairsIssue> {
        val issues = mutableListOf<StairsIssue>()
        val footprint = StairsGeometry.footprint(stairs)

        val below = rooms.filter { it.isPlaced && it.floorLevel == stairs.lowerLevel }
        if (below.isNotEmpty() && below.none { contains(it, footprint) }) {
            issues += StairsIssue.NotInsideRoomBelow
        }

        val above = rooms.filter { it.isPlaced && it.floorLevel == stairs.upperLevel }
        if (above.isNotEmpty() && above.none { contains(it, footprint) }) {
            issues += StairsIssue.NotInsideRoomAbove
        }

        for (other in allStairs) {
            if (other.id == stairs.id || other.lowerLevel != stairs.lowerLevel) continue
            if (overlap(footprint, StairsGeometry.footprint(other))) {
                issues += StairsIssue.OverlapsStairs(other.id)
            }
        }
        return issues
    }

    /** Identifikatori stepenica kod kojih provera položaja ([validate]) nalazi bar jedan problem. */
    fun idsWithIssues(allStairs: List<Stairs>, rooms: List<Room>): Set<String> =
        allStairs.filter { validate(it, allStairs, rooms).isNotEmpty() }.map { it.id }.toSet()

    /** Površina je unutar sobe (unutrašnjost sobe, uz toleranciju [RoomGeometry.EPSILON_M]). */
    private fun contains(room: Room, f: StairsFootprint): Boolean {
        val eps = RoomGeometry.EPSILON_M
        val x = room.xM!!
        val y = room.yM!!
        return f.x0 >= x - eps && f.x1 <= x + room.lengthM + eps &&
            f.y0 >= y - eps && f.y1 <= y + room.widthM + eps
    }

    /** Preklapanje je površina veća od nule: dodirivanje ivicom nije preklapanje. */
    private fun overlap(a: StairsFootprint, b: StairsFootprint): Boolean {
        val eps = RoomGeometry.EPSILON_M
        return minOf(a.x1, b.x1) - maxOf(a.x0, b.x0) > eps &&
            minOf(a.y1, b.y1) - maxOf(a.y0, b.y0) > eps
    }
}
