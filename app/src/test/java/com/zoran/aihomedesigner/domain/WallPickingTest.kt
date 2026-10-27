package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class WallPickingTest {

    private fun room(
        id: String, x: Double? = 1.0, y: Double? = 2.0, length: Double = 4.0, width: Double = 3.0
    ) = Room(
        id = id, projectId = "p", name = id, lengthM = length, widthM = width, heightM = 2.6,
        wallThicknessM = null, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = x, yM = y, floorLevel = 0
    )

    private val a = room("a") // x od 1 do 5, y od 2 do 5

    @Test
    fun tapNearTheNorthWallGivesThePositionAlongIt() {
        val tap = WallPicking.nearest(listOf(a), 3.0, 1.9, 0.3)!!
        assertEquals("a", tap.roomId)
        assertEquals(WallSide.NORTH, tap.side)
        assertEquals(2.0, tap.alongM, 1e-9)
        assertEquals(0.1, tap.distanceM, 1e-9)
    }

    @Test
    fun tapNearTheSouthWall() {
        val tap = WallPicking.nearest(listOf(a), 4.0, 5.1, 0.3)!!
        assertEquals(WallSide.SOUTH, tap.side)
        assertEquals(3.0, tap.alongM, 1e-9)
    }

    @Test
    fun tapNearTheEastWallIsMeasuredFromTheNorthCorner() {
        val tap = WallPicking.nearest(listOf(a), 5.05, 3.5, 0.3)!!
        assertEquals(WallSide.EAST, tap.side)
        assertEquals(1.5, tap.alongM, 1e-9)
    }

    @Test
    fun tapNearTheWestWallIsMeasuredFromTheNorthCorner() {
        val tap = WallPicking.nearest(listOf(a), 0.9, 4.0, 0.3)!!
        assertEquals(WallSide.WEST, tap.side)
        assertEquals(2.0, tap.alongM, 1e-9)
    }

    @Test
    fun tapFarFromEveryWallIsNotAHit() {
        assertNull(WallPicking.nearest(listOf(a), 3.0, 3.5, 0.3)) // sredina sobe
        assertNull(WallPicking.nearest(listOf(a), 9.0, 9.0, 0.3)) // daleko van
    }

    @Test
    fun toleranceIsInclusive() {
        // 2,0 - 1,75 = 0,25 tačno (bez grešaka zaokruživanja), pa je dodir tačno na granici tolerancije
        assertNotNull(WallPicking.nearest(listOf(a), 3.0, 1.75, 0.25))
        assertNull(WallPicking.nearest(listOf(a), 3.0, 1.74, 0.25))
    }

    @Test
    fun positionAlongTheWallIsClampedToTheWall() {
        // Dodir malo pored severozapadnog ugla: najbliža tačka je sam ugao
        val tap = WallPicking.nearest(listOf(a), 0.9, 1.95, 0.3)!!
        assertEquals(0.0, tap.alongM, 1e-9)
        assertEquals(WallSide.NORTH, tap.side) // pri izjednačenju prednost ima severni zid
    }

    @Test
    fun theNearestRoomWins() {
        val b = room("b", x = 10.0, y = 2.0)
        val tap = WallPicking.nearest(listOf(a, b), 9.9, 3.0, 0.3)!!
        assertEquals("b", tap.roomId)
        assertEquals(WallSide.WEST, tap.side)
    }

    @Test
    fun aSharedWallBelongsToTheRoomThatTheTapIsInside() {
        val left = room("left", x = 0.0, y = 0.0, length = 4.0, width = 3.0)
        val right = room("right", x = 4.0, y = 0.0, length = 3.0, width = 3.0)
        val insideLeft = WallPicking.nearest(listOf(left, right), 3.95, 1.5, 0.3)!!
        assertEquals("left", insideLeft.roomId)
        assertEquals(WallSide.EAST, insideLeft.side)
        val insideRight = WallPicking.nearest(listOf(left, right), 4.05, 1.5, 0.3)!!
        assertEquals("right", insideRight.roomId)
        assertEquals(WallSide.WEST, insideRight.side)
        // isto i kad je redosled soba obrnut
        val reversed = WallPicking.nearest(listOf(right, left), 3.95, 1.5, 0.3)!!
        assertEquals("left", reversed.roomId)
    }

    @Test
    fun roomsWithoutAPositionAreSkipped() {
        assertNull(WallPicking.nearest(listOf(room("u", x = null, y = null)), 0.0, 0.0, 5.0))
    }
}
