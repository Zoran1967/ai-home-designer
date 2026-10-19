package com.zoran.aihomedesigner.domain

import kotlin.math.max
import kotlin.math.min

/** Pravougaonik u koordinatama sobe (od gornjeg levog ugla unutrašnjosti sobe), u metrima. */
data class LocalRect(val x0: Double, val y0: Double, val x1: Double, val y1: Double)

object FurnitureGeometry {

    /** Dubina prolaza ispred vrata koja mora da ostane slobodna (60 cm). */
    const val DOOR_PASSAGE_DEPTH_M = 0.6

    private const val EPS = RoomGeometry.EPSILON_M

    fun rect(furniture: Furniture): LocalRect = LocalRect(
        furniture.xM,
        furniture.yM,
        furniture.xM + furniture.footprintLengthM,
        furniture.yM + furniture.footprintWidthM
    )

    /** Da li se unutrašnjosti dva pravougaonika preklapaju. Dodir ivicom se ne računa. */
    fun intersects(a: LocalRect, b: LocalRect): Boolean =
        a.x0 < b.x1 - EPS && b.x0 < a.x1 - EPS && a.y0 < b.y1 - EPS && b.y0 < a.y1 - EPS

    fun isInsideRoom(room: Room, furniture: Furniture): Boolean {
        val r = rect(furniture)
        return r.x0 >= -EPS && r.y0 >= -EPS &&
            r.x1 <= room.lengthM + EPS && r.y1 <= room.widthM + EPS
    }

    /** Površ ispred vrata, široka koliko i vrata, duboka [DOOR_PASSAGE_DEPTH_M], sa unutrašnje strane zida. */
    fun doorPassageRect(room: Room, opening: Opening): LocalRect {
        val a = opening.offsetM
        val b = opening.offsetM + opening.widthM
        val depth = DOOR_PASSAGE_DEPTH_M
        return when (opening.side) {
            WallSide.NORTH -> LocalRect(a, 0.0, b, depth)
            WallSide.SOUTH -> LocalRect(a, room.widthM - depth, b, room.widthM)
            WallSide.WEST -> LocalRect(0.0, a, depth, b)
            WallSide.EAST -> LocalRect(room.lengthM - depth, a, room.lengthM, b)
        }
    }

    /**
     * Da li nameštaj blokira vrata: ulazi u prolaz ispred vrata, ili (kod vrata koja se otvaraju u sobu)
     * u površinu koju vrata prebrišu pri otvaranju. Prozori se ne proveravaju.
     */
    fun blocksDoor(room: Room, furniture: Furniture, opening: Opening): Boolean {
        if (opening.type != OpeningType.DOOR) return false
        val r = rect(furniture)
        if (intersects(r, doorPassageRect(room, opening))) return true

        if (opening.swing != DoorSwing.INTO_ROOM) return false
        val shape = DoorSwingGeometry.shape(opening, room.copy(xM = 0.0, yM = 0.0)) ?: return false
        return intersectsQuarterDisc(r, shape)
    }

    /** Da li pravougaonik zalazi u četvrtinu kruga po kojoj se otvaraju vrata. */
    private fun intersectsQuarterDisc(r: LocalRect, shape: DoorSwingShape): Boolean {
        val radius = shape.radiusM
        val h = shape.hinge
        val ux = (shape.closedEnd.xM - h.xM) / radius
        val uy = (shape.closedEnd.yM - h.yM) / radius
        val vx = (shape.openEnd.xM - h.xM) / radius
        val vy = (shape.openEnd.yM - h.yM) / radius

        val xs = doubleArrayOf(r.x0, r.x1)
        val ys = doubleArrayOf(r.y0, r.y1)
        var uMin = Double.MAX_VALUE
        var uMax = -Double.MAX_VALUE
        var vMin = Double.MAX_VALUE
        var vMax = -Double.MAX_VALUE
        for (x in xs) {
            for (y in ys) {
                val u = (x - h.xM) * ux + (y - h.yM) * uy
                val v = (x - h.xM) * vx + (y - h.yM) * vy
                uMin = min(uMin, u)
                uMax = max(uMax, u)
                vMin = min(vMin, v)
                vMax = max(vMax, v)
            }
        }

        val u0 = max(uMin, 0.0)
        val v0 = max(vMin, 0.0)
        if (uMax - u0 <= EPS || vMax - v0 <= EPS) return false
        return u0 * u0 + v0 * v0 < radius * radius - EPS
    }
}
