package com.zoran.aihomedesigner.domain

import kotlin.math.max
import kotlin.math.min

/** Pravougaonik na planu, u metrima: (x0, y0) gornji levi, (x1, y1) donji desni ugao. */
data class PlanRect(val x0: Double, val y0: Double, val x1: Double, val y1: Double)

/** VERTICAL: zid je uspravna linija (sobe su jedna levo, druga desno). HORIZONTAL: sobe su jedna iznad druge. */
enum class WallOrientation { VERTICAL, HORIZONTAL }

/**
 * Zid između dve susedne sobe. Debljina zida je razmak između unutrašnjosti dve sobe.
 * Taj razmak proizlazi iz rasporeda koji je korisnik napravio na planu, nije posebno izmeren.
 *
 * rect je površina zida na planu. Za VERTICAL zid je širina rect-a debljina zida,
 * a visina je dužina zida koju sobe dele. Za HORIZONTAL zid je obrnuto.
 */
data class SharedWall(
    val roomAId: String,
    val roomBId: String,
    val orientation: WallOrientation,
    val thicknessM: Double,
    val rect: PlanRect
) {
    /** Dužina zida koju dve sobe dele. */
    val lengthM: Double
        get() = when (orientation) {
            WallOrientation.VERTICAL -> rect.y1 - rect.y0
            WallOrientation.HORIZONTAL -> rect.x1 - rect.x0
        }
}

object SharedWalls {

    /** Najveći razmak između soba koji se još računa kao zid između njih (60 cm). */
    const val MAX_GAP_M = 0.6

    /** Najmanja dužina kojom se dve sobe moraju "gledati" da bi se računale kao susedne (30 cm). */
    const val MIN_SHARED_LENGTH_M = 0.3

    private const val EPS = RoomGeometry.EPSILON_M

    /**
     * Nalazi parove susednih soba. Sobe koje se preklapaju nisu susedne (to je posebna greška).
     * Sobe bez položaja se ne razmatraju. Redosled soba u listi ne utiče na rezultat.
     */
    fun find(rooms: List<Room>): List<SharedWall> {
        val placed = rooms.filter { it.isPlaced }
        val result = mutableListOf<SharedWall>()
        for (i in placed.indices) {
            for (j in i + 1 until placed.size) {
                val a = placed[i]
                val b = placed[j]
                if (RoomGeometry.overlaps(a, b)) continue
                val wall = sideBySide(a, b) ?: stacked(a, b)
                if (wall != null) result += wall
            }
        }
        return result
    }

    /** Sobe jedna pored druge (levo-desno), zid je uspravan. */
    private fun sideBySide(a: Room, b: Room): SharedWall? {
        val (left, right) = if (a.xM!! <= b.xM!!) a to b else b to a
        val leftEdge = left.xM!! + left.lengthM
        val gap = right.xM!! - leftEdge
        val y0 = max(left.yM!!, right.yM!!)
        val y1 = min(left.yM!! + left.widthM, right.yM!! + right.widthM)
        if (gap < -EPS || gap > MAX_GAP_M || y1 - y0 < MIN_SHARED_LENGTH_M) return null
        val thickness = max(gap, 0.0)
        return SharedWall(
            roomAId = left.id,
            roomBId = right.id,
            orientation = WallOrientation.VERTICAL,
            thicknessM = thickness,
            rect = PlanRect(leftEdge, y0, leftEdge + thickness, y1)
        )
    }

    /** Sobe jedna iznad druge, zid je vodoravan. */
    private fun stacked(a: Room, b: Room): SharedWall? {
        val (top, bottom) = if (a.yM!! <= b.yM!!) a to b else b to a
        val topEdge = top.yM!! + top.widthM
        val gap = bottom.yM!! - topEdge
        val x0 = max(top.xM!!, bottom.xM!!)
        val x1 = min(top.xM!! + top.lengthM, bottom.xM!! + bottom.lengthM)
        if (gap < -EPS || gap > MAX_GAP_M || x1 - x0 < MIN_SHARED_LENGTH_M) return null
        val thickness = max(gap, 0.0)
        return SharedWall(
            roomAId = top.id,
            roomBId = bottom.id,
            orientation = WallOrientation.HORIZONTAL,
            thicknessM = thickness,
            rect = PlanRect(x0, topEdge, x1, topEdge + thickness)
        )
    }
}
