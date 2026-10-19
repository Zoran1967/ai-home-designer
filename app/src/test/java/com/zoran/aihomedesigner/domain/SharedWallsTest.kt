package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedWallsTest {

    private fun room(
        id: String,
        x: Double?,
        y: Double?,
        length: Double = 4.0,
        width: Double = 3.0,
        thickness: Double? = null
    ) = Room(
        id = id,
        projectId = "p",
        name = id,
        lengthM = length,
        widthM = width,
        heightM = 2.6,
        wallThicknessM = thickness,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L,
        xM = x,
        yM = y
    )

    @Test
    fun roomsSideBySideWithGapShareAVerticalWall() {
        val walls = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", 4.2, 0.0)))
        assertEquals(1, walls.size)
        val wall = walls[0]
        assertEquals(WallOrientation.VERTICAL, wall.orientation)
        assertEquals(0.2, wall.thicknessM, 1e-9)
        assertEquals("a", wall.roomAId)
        assertEquals("b", wall.roomBId)
        assertEquals(PlanRect(4.0, 0.0, 4.2, 3.0), wall.rect)
        assertEquals(3.0, wall.lengthM, 1e-9)
    }

    @Test
    fun roomsStackedWithGapShareAHorizontalWall() {
        val walls = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", 0.0, 3.25)))
        assertEquals(1, walls.size)
        val wall = walls[0]
        assertEquals(WallOrientation.HORIZONTAL, wall.orientation)
        assertEquals(0.25, wall.thicknessM, 1e-9)
        assertEquals(PlanRect(0.0, 3.0, 4.0, 3.25), wall.rect)
        assertEquals(4.0, wall.lengthM, 1e-9)
    }

    @Test
    fun orderOfRoomsInListDoesNotMatter() {
        val one = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", 4.2, 0.0)))
        val two = SharedWalls.find(listOf(room("b", 4.2, 0.0), room("a", 0.0, 0.0)))
        assertEquals(one, two)
    }

    @Test
    fun sharedLengthIsOnlyTheOverlappingPart() {
        val walls = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", 4.2, 2.0)))
        assertEquals(1, walls.size)
        assertEquals(1.0, walls[0].lengthM, 1e-9)
        assertEquals(PlanRect(4.0, 2.0, 4.2, 3.0), walls[0].rect)
    }

    @Test
    fun touchingRoomsHaveAWallOfZeroThickness() {
        val walls = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", 4.0, 0.0)))
        assertEquals(1, walls.size)
        assertEquals(0.0, walls[0].thicknessM, 1e-9)
    }

    @Test
    fun gapLargerThanLimitIsNotAWall() {
        val walls = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", 4.7, 0.0)))
        assertTrue(walls.isEmpty())
    }

    @Test
    fun tooShortOverlapIsNotAWall() {
        val walls = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", 4.2, 2.8)))
        assertTrue(walls.isEmpty())
    }

    @Test
    fun cornerToCornerRoomsAreNotNeighbours() {
        val walls = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", 4.2, 3.2)))
        assertTrue(walls.isEmpty())
    }

    @Test
    fun overlappingRoomsHaveNoSharedWall() {
        val walls = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", 3.0, 0.0)))
        assertTrue(walls.isEmpty())
    }

    @Test
    fun unplacedRoomsAreIgnored() {
        val walls = SharedWalls.find(listOf(room("a", 0.0, 0.0), room("b", null, null)))
        assertTrue(walls.isEmpty())
    }

    @Test
    fun threeRoomsInARowGiveTwoWalls() {
        val walls = SharedWalls.find(
            listOf(room("a", 0.0, 0.0), room("b", 4.2, 0.0), room("c", 8.4, 0.0))
        )
        assertEquals(2, walls.size)
    }
}
