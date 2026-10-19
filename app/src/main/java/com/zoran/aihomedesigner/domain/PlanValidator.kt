package com.zoran.aihomedesigner.domain

/** Problem pronađen pri proveri plana. */
sealed interface PlanIssue {

    /** Unutrašnjosti dve sobe se preklapaju. */
    data class RoomsOverlap(val roomAId: String, val roomBId: String) : PlanIssue

    /** Dve susedne sobe se dodiruju ili je razmak među njima manji od najmanje debljine zida. */
    data class WallTooThin(val wall: SharedWall) : PlanIssue

    /** Zid između soba je tanji od debljine zida koju je korisnik uneo za jednu od soba. */
    data class WallThinnerThanEntered(
        val wall: SharedWall,
        val roomId: String,
        val enteredThicknessM: Double
    ) : PlanIssue
}

/**
 * Provera plana. Proverava SAMO: preklapanje soba i debljinu zida između susednih soba.
 * Vrata, prozori, prolazi i nameštaj se ne proveravaju. Prazna lista problema znači samo da
 * ove dve provere nisu našle grešku, a ne da je plan ispravan u celini.
 */
object PlanValidator {

    /** Najmanja debljina zida koja se smatra zidom (5 cm), isto kao najmanja dozvoljena unesena debljina. */
    const val MIN_WALL_THICKNESS_M = DimensionLimits.MIN_THICKNESS_M

    /** Dozvoljeno odstupanje pri poređenju sa unetom debljinom zida (1 cm), zbog prevlačenja na planu. */
    const val THICKNESS_TOLERANCE_M = 0.01

    fun validate(rooms: List<Room>): List<PlanIssue> {
        val placed = rooms.filter { it.isPlaced }
        val issues = mutableListOf<PlanIssue>()

        for (i in placed.indices) {
            for (j in i + 1 until placed.size) {
                if (RoomGeometry.overlaps(placed[i], placed[j])) {
                    issues += PlanIssue.RoomsOverlap(placed[i].id, placed[j].id)
                }
            }
        }

        val byId = placed.associateBy { it.id }
        for (wall in SharedWalls.find(placed)) {
            if (wall.thicknessM < MIN_WALL_THICKNESS_M - RoomGeometry.EPSILON_M) {
                issues += PlanIssue.WallTooThin(wall)
                continue
            }
            for (roomId in listOf(wall.roomAId, wall.roomBId)) {
                val entered = byId[roomId]?.wallThicknessM ?: continue
                if (wall.thicknessM < entered - THICKNESS_TOLERANCE_M) {
                    issues += PlanIssue.WallThinnerThanEntered(wall, roomId, entered)
                }
            }
        }
        return issues
    }
}
