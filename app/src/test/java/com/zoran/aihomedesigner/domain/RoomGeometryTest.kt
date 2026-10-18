package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomGeometryTest {

    private fun room(
        id: String,
        x: Double?,
        y: Double?,
        length: Double = 4.0,
        width: Double = 3.0
    ) = Room(
        id = id,
        projectId = "p",
        name = id,
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
    fun partiallyOverlappingRoomsOverlap() {
        assertTrue(RoomGeometry.overlaps(room("a", 0.0, 0.0), room("b", 3.0, 1.0)))
    }

    @Test
    fun containedRoomOverlaps() {
        val big = room("a", 0.0, 0.0, length = 10.0, width = 10.0)
        val small = room("b", 2.0, 2.0, length = 1.0, width = 1.0)
        assertTrue(RoomGeometry.overlaps(big, small))
        assertTrue(RoomGeometry.overlaps(small, big))
    }

    @Test
    fun identicalRoomsOverlap() {
        assertTrue(RoomGeometry.overlaps(room("a", 1.0, 1.0), room("b", 1.0, 1.0)))
    }

    @Test
    fun roomsTouchingAtAnEdgeDoNotOverlap() {
        assertFalse(RoomGeometry.overlaps(room("a", 0.0, 0.0), room("b", 4.0, 0.0)))
        assertFalse(RoomGeometry.overlaps(room("a", 0.0, 0.0), room("b", 0.0, 3.0)))
    }

    @Test
    fun roomsTouchingAtACornerDoNotOverlap() {
        assertFalse(RoomGeometry.overlaps(room("a", 0.0, 0.0), room("b", 4.0, 3.0)))
    }

    @Test
    fun separateRoomsDoNotOverlap() {
        assertFalse(RoomGeometry.overlaps(room("a", 0.0, 0.0), room("b", 10.0, 10.0)))
    }

    @Test
    fun tinyOverlapAboveToleranceIsDetected() {
        assertTrue(RoomGeometry.overlaps(room("a", 0.0, 0.0), room("b", 3.99, 0.0)))
    }

    @Test
    fun unplacedRoomsAreNeverOverlapping() {
        assertFalse(RoomGeometry.overlaps(room("a", null, null), room("b", 0.0, 0.0)))
        assertFalse(RoomGeometry.overlaps(room("a", 0.0, null), room("b", 0.0, 0.0)))
    }

    @Test
    fun overlappingIdsContainOnlyRoomsThatOverlap() {
        val rooms = listOf(
            room("a", 0.0, 0.0),
            room("b", 3.0, 1.0),
            room("c", 20.0, 20.0)
        )
        assertEquals(setOf("a", "b"), RoomGeometry.overlappingRoomIds(rooms))
    }

    @Test
    fun noRoomsMeansNoOverlaps() {
        assertEquals(emptySet<String>(), RoomGeometry.overlappingRoomIds(emptyList()))
    }
}
