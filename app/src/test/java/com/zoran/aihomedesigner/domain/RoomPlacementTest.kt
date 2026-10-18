package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RoomPlacementTest {

    private fun room(
        length: Double = 4.0,
        width: Double = 3.0,
        x: Double? = null,
        y: Double? = null
    ) = Room(
        id = Room.newId(),
        projectId = "p",
        name = "Soba",
        lengthM = length,
        widthM = width,
        heightM = 2.6,
        wallThicknessM = null,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L,
        xM = x,
        yM = y
    )

    @Test
    fun firstRoomGoesToOrigin() {
        assertEquals(PlanPoint(0.0, 0.0), RoomPlacement.nextPosition(emptyList()))
    }

    @Test
    fun nextRoomGoesRightOfExistingRoomWithGap() {
        val p = RoomPlacement.nextPosition(listOf(room(length = 4.0, x = 0.0, y = 0.0)))
        assertEquals(4.0 + RoomPlacement.GAP_M, p.xM, 1e-9)
        assertEquals(0.0, p.yM, 1e-9)
    }

    @Test
    fun usesRightmostRoomEdgeAndTopmostY() {
        val rooms = listOf(
            room(length = 4.0, x = 0.0, y = 2.0),
            room(length = 3.0, x = 6.0, y = 1.0)
        )
        val p = RoomPlacement.nextPosition(rooms)
        assertEquals(9.0 + RoomPlacement.GAP_M, p.xM, 1e-9)
        assertEquals(1.0, p.yM, 1e-9)
    }

    @Test
    fun roomsWithoutPositionAreIgnored() {
        val rooms = listOf(room(length = 10.0), room(length = 2.0, x = 1.0, y = 0.0))
        val p = RoomPlacement.nextPosition(rooms)
        assertEquals(3.0 + RoomPlacement.GAP_M, p.xM, 1e-9)
    }

    @Test
    fun onlyUnplacedRoomsMeansOrigin() {
        assertEquals(PlanPoint(0.0, 0.0), RoomPlacement.nextPosition(listOf(room(), room())))
    }

    @Test
    fun roomWithOnlyOneCoordinateCountsAsUnplaced() {
        assertEquals(false, room(x = 1.0, y = null).isPlaced)
        assertEquals(true, room(x = 1.0, y = 2.0).isPlaced)
    }
}
