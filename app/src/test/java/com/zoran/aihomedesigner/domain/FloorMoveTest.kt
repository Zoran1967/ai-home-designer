package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorMoveTest {

    private fun room(
        id: String,
        name: String = id,
        level: Int = 0,
        x: Double? = 0.0,
        y: Double? = 0.0,
        length: Double = 4.0
    ) = Room(
        id = id, projectId = "p", name = name,
        lengthM = length, widthM = 3.0, heightM = 2.6,
        wallThicknessM = 0.2, dimensionSource = DimensionSource.IMPORTED_CALIBRATED,
        createdAtMillis = 1L, xM = x, yM = y, floorLevel = level
    )

    @Test
    fun movesToAnEmptyFloorAtOrigin() {
        val a = room("Dnevna", level = 0, x = 5.0, y = 7.0)
        val result = FloorMove.move(a, 1, listOf(a))
        val moved = (result as FloorMoveResult.Moved).room
        assertEquals(1, moved.floorLevel)
        assertEquals(0.0, moved.xM!!, 0.0)
        assertEquals(0.0, moved.yM!!, 0.0)
    }

    @Test
    fun movedRoomIsPlacedRightOfRoomsOnTheTargetFloor() {
        val a = room("Soba", level = 0)
        val upper = room("Spavaca", level = 1, x = 2.0, y = 1.0, length = 5.0)
        val moved = (FloorMove.move(a, 1, listOf(a, upper)) as FloorMoveResult.Moved).room
        assertEquals(2.0 + 5.0 + RoomPlacement.GAP_M, moved.xM!!, 1e-9)
        assertEquals(1.0, moved.yM!!, 1e-9)
    }

    @Test
    fun measurementsAndSourceAreUnchanged() {
        val a = room("Soba", level = 0)
        val moved = (FloorMove.move(a, -1, listOf(a)) as FloorMoveResult.Moved).room
        assertEquals(a.lengthM, moved.lengthM, 0.0)
        assertEquals(a.widthM, moved.widthM, 0.0)
        assertEquals(a.heightM, moved.heightM, 0.0)
        assertEquals(a.wallThicknessM, moved.wallThicknessM)
        assertEquals(a.dimensionSource, moved.dimensionSource)
        assertEquals(a.id, moved.id)
        assertEquals(a.name, moved.name)
    }

    @Test
    fun sameFloorIsRejected() {
        val a = room("Soba", level = 1)
        assertEquals(FloorMoveResult.SameFloor, FloorMove.move(a, 1, listOf(a)))
    }

    @Test
    fun floorsOutsideTheLimitsAreRejected() {
        val a = room("Soba")
        assertEquals(FloorMoveResult.InvalidFloor, FloorMove.move(a, Floors.MAX_LEVEL + 1, listOf(a)))
        assertEquals(FloorMoveResult.InvalidFloor, FloorMove.move(a, Floors.MIN_LEVEL - 1, listOf(a)))
    }

    @Test
    fun sameNameOnTheTargetFloorIsRejectedIgnoringCaseAndSpaces() {
        val a = room("Kupatilo", level = 0)
        val other = room("b", name = "  kupatilo ", level = 1)
        assertEquals(FloorMoveResult.NameTaken, FloorMove.move(a, 1, listOf(a, other)))
    }

    @Test
    fun sameNameOnAnotherFloorIsAllowed() {
        val a = room("Kupatilo", level = 0)
        val other = room("b", name = "Kupatilo", level = 2)
        assertTrue(FloorMove.move(a, 1, listOf(a, other)) is FloorMoveResult.Moved)
    }

    @Test
    fun unplacedRoomsOnTheTargetFloorAreIgnoredForPosition() {
        val a = room("Soba", level = 0)
        val loose = room("Nepostavljena", level = 1, x = null, y = null)
        val moved = (FloorMove.move(a, 1, listOf(a, loose)) as FloorMoveResult.Moved).room
        assertEquals(0.0, moved.xM!!, 0.0)
        assertEquals(0.0, moved.yM!!, 0.0)
    }
}
