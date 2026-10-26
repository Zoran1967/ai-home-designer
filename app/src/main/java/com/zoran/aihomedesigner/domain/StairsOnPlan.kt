package com.zoran.aihomedesigner.domain

/** Kako se stepenice odnose prema spratu koji se prikazuje na planu. */
enum class StairsRole {
    /** Stepenice kreću sa ovog sprata (ovo je njihov donji sprat). */
    DEPARTS,

    /** Stepenice stižu na ovaj sprat (ovo je njihov gornji sprat). */
    ARRIVES
}

/** Duž na planu, u metrima, u koordinatama plana. */
data class StairsLine(val from: PlanPoint, val to: PlanPoint)

/** Geometrija crtanja stepenica na 2D planu. Čista logika, bez Androida. */
object StairsOnPlan {

    /** Uloga stepenica na spratu [floorLevel], ili null ako stepenice ne dodiruju taj sprat. */
    fun role(stairs: Stairs, floorLevel: Int): StairsRole? = when (floorLevel) {
        stairs.lowerLevel -> StairsRole.DEPARTS
        stairs.upperLevel -> StairsRole.ARRIVES
        else -> null
    }

    /**
     * Poprečne crte između gazišta. Za [stepCount] stepenika ima [stepCount] - 1 gazišta,
     * pa je unutrašnjih crta [stepCount] - 2. Broj stepenika je procena (vidi [StairsPlanning.riseM]).
     */
    fun stepLines(stairs: Stairs, stepCount: Int): List<StairsLine> {
        require(stepCount >= 2) { "Stepenice imaju najmanje 2 stepenika" }
        val f = StairsGeometry.footprint(stairs)
        val treads = stepCount - 1
        val runsAlongX = stairs.directionDeg % 180 == 0
        return (1 until treads).map { i ->
            val t = i.toDouble() / treads
            if (runsAlongX) {
                val x = f.x0 + (f.x1 - f.x0) * t
                StairsLine(PlanPoint(x, f.y0), PlanPoint(x, f.y1))
            } else {
                val y = f.y0 + (f.y1 - f.y0) * t
                StairsLine(PlanPoint(f.x0, y), PlanPoint(f.x1, y))
            }
        }
    }

    /** Strelica po sredini stepenica, od dna ka vrhu (u pravcu penjanja). */
    fun climbArrow(stairs: Stairs): StairsLine {
        val f = StairsGeometry.footprint(stairs)
        val cx = (f.x0 + f.x1) / 2
        val cy = (f.y0 + f.y1) / 2
        return when (stairs.directionDeg) {
            0 -> StairsLine(PlanPoint(f.x0, cy), PlanPoint(f.x1, cy))
            90 -> StairsLine(PlanPoint(cx, f.y0), PlanPoint(cx, f.y1))
            180 -> StairsLine(PlanPoint(f.x1, cy), PlanPoint(f.x0, cy))
            270 -> StairsLine(PlanPoint(cx, f.y1), PlanPoint(cx, f.y0))
            else -> throw IllegalArgumentException("Pravac penjanja mora biti 0, 90, 180 ili 270")
        }
    }
}
