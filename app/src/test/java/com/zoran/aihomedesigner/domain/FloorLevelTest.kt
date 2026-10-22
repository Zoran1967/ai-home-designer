package com.zoran.aihomedesigner.domain

import com.zoran.aihomedesigner.data.RoomEntity
import com.zoran.aihomedesigner.data.toDomainOrNull
import com.zoran.aihomedesigner.data.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FloorLevelTest {

    private fun room(id: String, level: Int = 0) = Room(
        id = id, projectId = "p", name = id,
        lengthM = 4.0, widthM = 3.0, heightM = 2.6,
        wallThicknessM = null, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = 0.0, yM = 0.0, floorLevel = level
    )

    @Test
    fun aRoomWithoutAnExplicitFloorIsOnTheGroundFloor() {
        val room = Room("r", "p", "Soba", 4.0, 3.0, 2.6, null, DimensionSource.USER_CONFIRMED, 1L)
        assertEquals(0, room.floorLevel)
    }

    @Test
    fun levelsAreSortedDistinctAndAlwaysIncludeTheGroundFloor() {
        assertEquals(listOf(-1, 0, 1), Floors.levels(listOf(room("a", 1), room("b", 1), room("c", -1))))
        assertEquals(listOf(0), Floors.levels(emptyList()))
        assertEquals(listOf(0, 2), Floors.levels(listOf(room("a", 2))))
    }

    @Test
    fun nextFloorsAboveAndBelowFollowTheExistingOnes() {
        val rooms = listOf(room("a", 0), room("b", 1))
        assertEquals(2, Floors.nextAbove(rooms))
        assertEquals(-1, Floors.nextBelow(rooms))
        assertEquals(1, Floors.nextAbove(emptyList()))
    }

    @Test
    fun floorsBeyondTheLimitsAreNotOffered() {
        assertNull(Floors.nextAbove(listOf(room("a", Floors.MAX_LEVEL))))
        assertNull(Floors.nextBelow(listOf(room("a", Floors.MIN_LEVEL))))
    }

    @Test
    fun levelValidityFollowsTheLimits() {
        assertTrue(Floors.isValidLevel(0))
        assertTrue(Floors.isValidLevel(Floors.MAX_LEVEL))
        assertTrue(Floors.isValidLevel(Floors.MIN_LEVEL))
        assertFalse(Floors.isValidLevel(Floors.MAX_LEVEL + 1))
        assertFalse(Floors.isValidLevel(Floors.MIN_LEVEL - 1))
    }

    @Test
    fun floorLevelSurvivesTheDatabaseRoundTrip() {
        val original = room("a", level = 2)
        assertEquals(original, original.toEntity().toDomainOrNull())
        assertEquals(2, original.toEntity().floorLevel)
    }

    @Test
    fun anEntityWithoutAFloorDefaultsToTheGroundFloor() {
        val entity = RoomEntity("r", "p", "Soba", 4.0, 3.0, 2.6, null, "USER_CONFIRMED", 1L)
        assertEquals(0, entity.floorLevel)
        assertEquals(0, entity.toDomainOrNull()!!.floorLevel)
    }
}
