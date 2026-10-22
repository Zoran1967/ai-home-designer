package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorPlacementTest {

    private fun room(id: String, level: Int, x: Double? = null, y: Double? = null, created: Long = 1L) = Room(
        id = id, projectId = "p", name = id,
        lengthM = 4.0, widthM = 3.0, heightM = 2.6,
        wallThicknessM = null, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = created, xM = x, yM = y, floorLevel = level
    )

    @Test
    fun roomOnANewFloorStartsAtTheOriginEvenIfAnotherFloorIsBusy() {
        val rooms = listOf(room("a", 0, 0.0, 0.0), room("b", 1))
        val placed = RoomPlacement.autoPlaceByFloor(rooms)
        assertEquals(listOf("b"), placed.map { it.id })
        assertEquals(0.0, placed[0].xM!!, 1e-9)
        assertEquals(0.0, placed[0].yM!!, 1e-9)
    }

    @Test
    fun roomOnTheSameFloorGoesNextToTheExistingOne() {
        val rooms = listOf(room("a", 0, 0.0, 0.0), room("c", 0))
        val placed = RoomPlacement.autoPlaceByFloor(rooms)
        assertEquals(1, placed.size)
        assertEquals(4.0 + RoomPlacement.GAP_M, placed[0].xM!!, 1e-9)
    }

    @Test
    fun eachFloorIsPlacedIndependently() {
        val rooms = listOf(
            room("a", 0, created = 1L), room("b", 0, created = 2L),
            room("c", 1, created = 3L), room("d", 1, created = 4L)
        )
        val placed = RoomPlacement.autoPlaceByFloor(rooms).associateBy { it.id }
        assertEquals(0.0, placed.getValue("a").xM!!, 1e-9)
        assertEquals(0.0, placed.getValue("c").xM!!, 1e-9)
        assertEquals(placed.getValue("b").xM!!, placed.getValue("d").xM!!, 1e-9)
        assertTrue(placed.values.all { it.isPlaced })
    }

    @Test
    fun nothingIsReturnedWhenAllRoomsAreAlreadyPlaced() {
        val rooms = listOf(room("a", 0, 0.0, 0.0), room("b", 1, 0.0, 0.0))
        assertTrue(RoomPlacement.autoPlaceByFloor(rooms).isEmpty())
    }
}
