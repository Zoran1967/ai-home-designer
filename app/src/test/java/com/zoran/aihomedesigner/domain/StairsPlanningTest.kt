package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StairsPlanningTest {

    private fun room(
        id: String, level: Int = 0, height: Double = 2.6, x: Double? = null, y: Double? = null
    ) = Room(
        id = id, projectId = "p", name = id, lengthM = 4.0, widthM = 3.0, heightM = height,
        wallThicknessM = null, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = x, yM = y, floorLevel = level
    )

    @Test
    fun riseIsTheTallestRoomOnTheLowerFloorPlusTheSlab() {
        val rooms = listOf(room("a", 0, 2.5), room("b", 0, 2.8), room("c", 1, 3.5))
        assertEquals(2.8 + SceneBuilder.FLOOR_SLAB_M, StairsPlanning.riseM(rooms, 0), 1e-9)
        assertEquals(3.5 + SceneBuilder.FLOOR_SLAB_M, StairsPlanning.riseM(rooms, 1), 1e-9)
    }

    @Test
    fun riseUsesTheDefaultHeightForAFloorWithoutRooms() {
        assertEquals(
            SceneBuilder.DEFAULT_FLOOR_HEIGHT_M + SceneBuilder.FLOOR_SLAB_M,
            StairsPlanning.riseM(listOf(room("a", 0)), 3),
            1e-9
        )
        assertEquals(
            SceneBuilder.DEFAULT_FLOOR_HEIGHT_M + SceneBuilder.FLOOR_SLAB_M,
            StairsPlanning.riseM(emptyList(), 0),
            1e-9
        )
    }

    @Test
    fun defaultSpotIsTheCornerOfTheFirstPlacedRoomOnTheLowerFloor() {
        val rooms = listOf(
            room("unplaced", 0),
            room("upper", 1, x = 9.0, y = 9.0),
            room("first", 0, x = 2.0, y = 1.5),
            room("second", 0, x = 7.0, y = 7.0)
        )
        assertEquals(PlanPoint(2.0, 1.5), StairsPlanning.defaultSpot(rooms, 0))
        assertEquals(PlanPoint(9.0, 9.0), StairsPlanning.defaultSpot(rooms, 1))
    }

    @Test
    fun defaultSpotFallsBackToTheOrigin() {
        assertEquals(PlanPoint(0.0, 0.0), StairsPlanning.defaultSpot(emptyList(), 0))
        assertEquals(PlanPoint(0.0, 0.0), StairsPlanning.defaultSpot(listOf(room("a", 0)), 0))
    }
}
