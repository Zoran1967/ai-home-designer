package com.zoran.aihomedesigner.domain

import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/** Problem pronađen pri proveri nameštaja u jednoj sobi. */
sealed interface FurnitureIssue {
    /** Predmet izlazi iz sobe (preko zida). */
    data class OutsideRoom(val furnitureId: String) : FurnitureIssue

    /** Predmet je viši od sobe. */
    data class TooTall(val furnitureId: String) : FurnitureIssue

    /** Dva predmeta se preklapaju. */
    data class OverlapsFurniture(val furnitureAId: String, val furnitureBId: String) : FurnitureIssue

    /** Predmet stoji tamo gde se vrata otvaraju u sobu. */
    data class BlocksDoorSwing(val furnitureId: String, val openingId: String) : FurnitureIssue

    /** Predmet stoji neposredno ispred vrata i zatvara prolaz. */
    data class BlocksDoorway(val furnitureId: String, val openingId: String) : FurnitureIssue
}

/**
 * Provera nameštaja u jednoj sobi. Proverava SAMO: da li predmet staje u sobu, da li je viši od sobe,
 * da li se preklapa sa drugim nameštajem, da li stoji u prostoru otvaranja vrata i da li zatvara
 * prolaz ispred vrata. Širina prolaza između predmeta, prozori i drugi uslovi se ne proveravaju.
 * Prazan spisak znači samo da ove provere nisu našle problem, a ne da je raspored dobar u celini.
 *
 * Svi položaji su u koordinatama sobe (gornji levi ugao unutrašnjosti sobe je (0, 0)).
 */
object FurnitureChecker {

    /** Dubina slobodnog prostora ispred vrata, u metrima (60 cm). */
    const val DOOR_CLEARANCE_M = 0.6

    /** Korak pri traženju slobodnog mesta za novi predmet, u metrima. */
    const val SEARCH_STEP_M = 0.1

    private const val EPS = RoomGeometry.EPSILON_M

    private data class Rect(val x0: Double, val y0: Double, val x1: Double, val y1: Double)

    private fun footprint(f: Furniture) =
        Rect(f.xM, f.yM, f.xM + f.footprintLengthM, f.yM + f.footprintWidthM)

    private fun overlap(a: Rect, b: Rect) =
        a.x0 < b.x1 - EPS && b.x0 < a.x1 - EPS && a.y0 < b.y1 - EPS && b.y0 < a.y1 - EPS

    /** Soba pomerena u (0, 0), da se geometrija vrata računa u koordinatama sobe. */
    private fun atOrigin(room: Room) = room.copy(xM = 0.0, yM = 0.0)

    /** Prostor ispred vrata, uz zid, širine vrata i dubine [DOOR_CLEARANCE_M]. */
    private fun doorZone(opening: Opening, room: Room): Rect {
        val a = opening.offsetM
        val b = opening.offsetM + opening.widthM
        val d = DOOR_CLEARANCE_M
        return when (opening.side) {
            WallSide.NORTH -> Rect(a, 0.0, b, d)
            WallSide.SOUTH -> Rect(a, room.widthM - d, b, room.widthM)
            WallSide.WEST -> Rect(0.0, a, d, b)
            WallSide.EAST -> Rect(room.lengthM - d, a, room.lengthM, b)
        }
    }

    /**
     * Da li pravougaonik zadire u prostor koji vrata prebrišu pri otvaranju u sobu
     * (četvrtina kruga poluprečnika jednakog širini vrata oko šarki).
     */
    private fun blocksSwing(rect: Rect, shape: DoorSwingShape): Boolean {
        val w = shape.radiusM
        // Osa 1 ide od šarki ka zatvorenom kraju vrata, osa 2 ka otvorenom kraju. Obe su paralelne osama plana.
        val e1x = (shape.closedEnd.xM - shape.hinge.xM) / w
        val e1y = (shape.closedEnd.yM - shape.hinge.yM) / w
        val e2x = (shape.openEnd.xM - shape.hinge.xM) / w
        val e2y = (shape.openEnd.yM - shape.hinge.yM) / w

        fun u(x: Double, y: Double) = (x - shape.hinge.xM) * e1x + (y - shape.hinge.yM) * e1y
        fun v(x: Double, y: Double) = (x - shape.hinge.xM) * e2x + (y - shape.hinge.yM) * e2y

        val uA = u(rect.x0, rect.y0)
        val uB = u(rect.x1, rect.y1)
        val vA = v(rect.x0, rect.y0)
        val vB = v(rect.x1, rect.y1)

        // Presek pravougaonika sa četvrtinom ravni (u >= 0, v >= 0)
        val uLow = max(min(uA, uB), 0.0)
        val uHigh = max(uA, uB)
        val vLow = max(min(vA, vB), 0.0)
        val vHigh = max(vA, vB)
        if (uHigh - uLow <= EPS || vHigh - vLow <= EPS) return false

        // Najbliža tačka preseka šarkama je njegov ugao (uLow, vLow)
        return hypot(uLow, vLow) < w - EPS
    }

    /** Da li pravougaonik smeta vratima: ulazi u prostor otvaranja ili zatvara prolaz ispred njih. */
    private fun doorProblems(
        rect: Rect,
        room: Room,
        openings: List<Opening>
    ): List<Pair<Opening, Boolean>> {
        val result = mutableListOf<Pair<Opening, Boolean>>() // (vrata, true = prostor otvaranja)
        for (opening in openings) {
            if (opening.type != OpeningType.DOOR) continue
            val shape = DoorSwingGeometry.shape(opening, atOrigin(room))
            if (shape != null && opening.swing == DoorSwing.INTO_ROOM && blocksSwing(rect, shape)) {
                result += opening to true
            }
            if (overlap(rect, doorZone(opening, room))) {
                result += opening to false
            }
        }
        return result
    }

    fun check(furniture: List<Furniture>, room: Room, openings: List<Opening>): List<FurnitureIssue> {
        val issues = mutableListOf<FurnitureIssue>()

        for (item in furniture) {
            val rect = footprint(item)
            if (rect.x0 < -EPS || rect.y0 < -EPS ||
                rect.x1 > room.lengthM + EPS || rect.y1 > room.widthM + EPS
            ) {
                issues += FurnitureIssue.OutsideRoom(item.id)
            }
            if (item.heightM > room.heightM + EPS) {
                issues += FurnitureIssue.TooTall(item.id)
            }
            for ((opening, isSwing) in doorProblems(rect, room, openings)) {
                issues += if (isSwing) {
                    FurnitureIssue.BlocksDoorSwing(item.id, opening.id)
                } else {
                    FurnitureIssue.BlocksDoorway(item.id, opening.id)
                }
            }
        }

        for (i in furniture.indices) {
            for (j in i + 1 until furniture.size) {
                if (overlap(footprint(furniture[i]), footprint(furniture[j]))) {
                    issues += FurnitureIssue.OverlapsFurniture(furniture[i].id, furniture[j].id)
                }
            }
        }
        return issues
    }

    /**
     * Traži prvo slobodno mesto za novi predmet tlocrta [footprintLengthM] x [footprintWidthM]:
     * unutar sobe, bez preklapanja sa drugim nameštajem i bez smetanja vratima.
     * Pretražuje se red po red sa korakom od [SEARCH_STEP_M]. Vraća null ako mesta nema.
     */
    fun findFreeSpot(
        room: Room,
        others: List<Furniture>,
        openings: List<Opening>,
        footprintLengthM: Double,
        footprintWidthM: Double
    ): PlanPoint? {
        val maxX = room.lengthM - footprintLengthM
        val maxY = room.widthM - footprintWidthM
        if (maxX < -EPS || maxY < -EPS) return null

        val otherRects = others.map { footprint(it) }
        var yi = 0
        while (true) {
            val y = RoomPlacement.roundToMm(yi * SEARCH_STEP_M)
            if (y > maxY + EPS) break
            var xi = 0
            while (true) {
                val x = RoomPlacement.roundToMm(xi * SEARCH_STEP_M)
                if (x > maxX + EPS) break
                val rect = Rect(x, y, x + footprintLengthM, y + footprintWidthM)
                val free = otherRects.none { overlap(rect, it) } &&
                    doorProblems(rect, room, openings).isEmpty()
                if (free) return PlanPoint(x, y)
                xi++
            }
            yi++
        }
        return null
    }
}
