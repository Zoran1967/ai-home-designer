package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPlacementTest {

    private fun room(
        length: Double = 4.0,
        width: Double = 3.0,
        x: Double? = null,
        y: Double? = null,
        id: String = Room.newId(),
        created: Long = 1L
    ) = Room(
        id = id,
        projectId = "p",
        name = "Soba",
        lengthM = length,
        widthM = width,
        heightM = 2.6,
        wallThicknessM = null,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = created,
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

    @Test
    fun autoPlaceGivesPositionsInOrderOfCreationWithoutOverlap() {
        val rooms = listOf(
            room(length = 4.0, id = "b", created = 2L),
            room(length = 3.0, id = "a", created = 1L),
            room(length = 2.0, id = "c", created = 3L)
        )
        val placed = RoomPlacement.autoPlace(rooms)
        assertEquals(listOf("a", "b", "c"), placed.map { it.id })
        assertTrue(placed.all { it.isPlaced })
        assertEquals(0.0, placed[0].xM!!, 1e-9)
        assertEquals(3.0 + RoomPlacement.GAP_M, placed[1].xM!!, 1e-9)
        assertEquals(3.0 + RoomPlacement.GAP_M + 4.0 + RoomPlacement.GAP_M, placed[2].xM!!, 1e-9)
        assertTrue(RoomGeometry.overlappingRoomIds(placed).isEmpty())
    }

    @Test
    fun autoPlaceKeepsExistingPlacedRoomsAndPlacesNewOnesAfterThem() {
        val rooms = listOf(
            room(length = 5.0, x = 2.0, y = 1.0, id = "old", created = 1L),
            room(length = 3.0, id = "new", created = 2L)
        )
        val placed = RoomPlacement.autoPlace(rooms)
        assertEquals(listOf("new"), placed.map { it.id })
        assertEquals(7.0 + RoomPlacement.GAP_M, placed[0].xM!!, 1e-9)
        assertEquals(1.0, placed[0].yM!!, 1e-9)
    }

    @Test
    fun autoPlaceDoesNothingWhenAllRoomsAreAlreadyPlaced() {
        val rooms = listOf(room(x = 0.0, y = 0.0), room(x = 6.0, y = 0.0))
        assertTrue(RoomPlacement.autoPlace(rooms).isEmpty())
    }

    @Test
    fun snapToGridRoundsToFiveCentimetres() {
        assertEquals(1.25, RoomPlacement.snapToGrid(1.23), 0.0)
        assertEquals(1.2, RoomPlacement.snapToGrid(1.22), 0.0)
        assertEquals(-0.05, RoomPlacement.snapToGrid(-0.04), 0.0)
        assertEquals(0.0, RoomPlacement.snapToGrid(0.01), 0.0)
        assertEquals(5.0, RoomPlacement.snapToGrid(5.0), 0.0)
    }

    @Test
    fun roundToMmRemovesFloatingPointNoise() {
        assertEquals(5.1, RoomPlacement.roundToMm(5.1000000000000005), 0.0)
        assertEquals(2.346, RoomPlacement.roundToMm(2.3456), 0.0)
    }

    @Test
    fun coordinateValidationRejectsNonFiniteAndHugeValues() {
        assertTrue(RoomPlacement.isValidCoordinate(0.0))
        assertTrue(RoomPlacement.isValidCoordinate(-999.9))
        assertEquals(false, RoomPlacement.isValidCoordinate(Double.NaN))
        assertEquals(false, RoomPlacement.isValidCoordinate(Double.POSITIVE_INFINITY))
        assertEquals(false, RoomPlacement.isValidCoordinate(1000.5))
    }

    @Test
    fun clampKeepsValuesInsideLimits() {
        assertEquals(1000.0, RoomPlacement.clampCoordinate(5000.0), 0.0)
        assertEquals(-1000.0, RoomPlacement.clampCoordinate(-5000.0), 0.0)
        assertEquals(3.5, RoomPlacement.clampCoordinate(3.5), 0.0)
    }
}
