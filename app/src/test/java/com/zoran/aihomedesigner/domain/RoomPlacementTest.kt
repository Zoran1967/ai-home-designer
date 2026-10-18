package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPlacementTest {

    private fun room(id: String, length: Double, width: Double, x: Double, y: Double) = Room(
        id = id,
        projectId = "p",
        name = id,
        lengthM = length,
        widthM = width,
        heightM = 2.6,
        wallThicknessM = null,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L,
        originXM = x,
        originYM = y
    )

    private fun overlaps(a: Room, b: Room): Boolean =
        a.originXM < b.rightM && b.originXM < a.rightM &&
            a.originYM < b.bottomM && b.originYM < a.bottomM

    @Test
    fun firstRoomStartsAtOrigin() {
        val origin = RoomPlacement.nextOrigin(emptyList())
        assertEquals(0.0, origin.xM, 0.0)
        assertEquals(0.0, origin.yM, 0.0)
    }

    @Test
    fun nextRoomIsPlacedToTheRightWithGap() {
        val origin = RoomPlacement.nextOrigin(listOf(room("a", 4.5, 3.2, 0.0, 0.0)))
        assertEquals(4.5 + RoomPlacement.GAP_M, origin.xM, 1e-9)
        assertEquals(0.0, origin.yM, 0.0)
    }

    @Test
    fun rightmostRoomDecidesThePosition() {
        val rooms = listOf(
            room("a", 4.0, 3.0, 0.0, 0.0),
            room("b", 2.0, 2.0, 10.0, 5.0),
            room("c", 3.0, 3.0, 4.5, 0.0)
        )
        val origin = RoomPlacement.nextOrigin(rooms)
        assertEquals(12.0 + RoomPlacement.GAP_M, origin.xM, 1e-9)
    }

    @Test
    fun newRoomNeverOverlapsExistingRooms() {
        val existing = mutableListOf<Room>()
        val sizes = listOf(4.5 to 3.2, 3.0 to 2.4, 2.0 to 1.2, 6.0 to 5.0, 0.5 to 0.5)
        sizes.forEachIndexed { index, (length, width) ->
            val origin = RoomPlacement.nextOrigin(existing)
            val created = room("r$index", length, width, origin.xM, origin.yM)
            assertTrue("Soba $index se preklapa", existing.none { overlaps(it, created) })
            existing.add(created)
        }
    }

    @Test
    fun roomEdgesAreOriginPlusSize() {
        val r = room("a", 4.0, 2.5, 1.0, 2.0)
        assertEquals(5.0, r.rightM, 1e-9)
        assertEquals(4.5, r.bottomM, 1e-9)
    }
}
