package com.zoran.aihomedesigner.domain

/**
 * Oblik otvaranja jednih vrata na planu, u metrima.
 *
 * hinge je tačka šarki, closedEnd je slobodan kraj zatvorenih vrata (na zidu),
 * openEnd je slobodan kraj potpuno otvorenih vrata. Slobodan kraj vrata opisuje četvrtinu kruga
 * poluprečnika radiusM oko tačke šarki, od closedEnd do openEnd.
 */
data class DoorSwingShape(
    val hinge: PlanPoint,
    val closedEnd: PlanPoint,
    val openEnd: PlanPoint,
    val radiusM: Double
)

/** Početak zida, jedinični vektor duž zida (dx, dy) i ka unutrašnjosti sobe (nx, ny). */
private data class WallFrame(
    val startX: Double,
    val startY: Double,
    val dx: Double,
    val dy: Double,
    val nx: Double,
    val ny: Double
)

object DoorSwingGeometry {

    /**
     * Računa oblik otvaranja za vrata na zidu sobe.
     * Vraća null za prozore, klizna vrata, vrata bez zadate strane šarki i sobe bez položaja na planu.
     */
    fun shape(opening: Opening, room: Room): DoorSwingShape? {
        if (opening.type != OpeningType.DOOR) return null
        val swing = opening.swing ?: return null
        if (swing == DoorSwing.SLIDING) return null
        val hingeEnd = opening.hinge ?: return null
        val roomX = room.xM ?: return null
        val roomY = room.yM ?: return null

        // Početak zida, smer duž zida (d) i smer ka unutrašnjosti sobe (n), u koordinatama plana
        val frame = when (opening.side) {
            WallSide.NORTH -> WallFrame(roomX, roomY, 1.0, 0.0, 0.0, 1.0)
            WallSide.SOUTH -> WallFrame(roomX, roomY + room.widthM, 1.0, 0.0, 0.0, -1.0)
            WallSide.WEST -> WallFrame(roomX, roomY, 0.0, 1.0, 1.0, 0.0)
            WallSide.EAST -> WallFrame(roomX + room.lengthM, roomY, 0.0, 1.0, -1.0, 0.0)
        }

        fun onWall(distance: Double) = PlanPoint(
            frame.startX + frame.dx * distance,
            frame.startY + frame.dy * distance
        )

        val near = onWall(opening.offsetM)
        val far = onWall(opening.offsetM + opening.widthM)
        val hinge = if (hingeEnd == HingeEnd.START) near else far
        val closedEnd = if (hingeEnd == HingeEnd.START) far else near

        val direction = if (swing == DoorSwing.INTO_ROOM) 1.0 else -1.0
        val openEnd = PlanPoint(
            hinge.xM + frame.nx * direction * opening.widthM,
            hinge.yM + frame.ny * direction * opening.widthM
        )
        return DoorSwingShape(hinge, closedEnd, openEnd, opening.widthM)
    }
}
