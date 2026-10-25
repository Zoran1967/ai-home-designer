package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomGroupsTest {

    private fun room(
        id: String,
        x: Double?,
        y: Double?,
        group: String? = null,
        locked: Boolean = false,
        level: Int = 0
    ) = Room(
        id = id, projectId = "p", name = id,
        lengthM = 4.0, widthM = 3.0, heightM = 2.6,
        wallThicknessM = 0.2, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = x, yM = y, floorLevel = level,
        positionLocked = locked, glueGroupId = group
    )

    @Test
    fun newRoomsAreNeitherLockedNorGlued() {
        val r = Room("r", "p", "Soba", 4.0, 3.0, 2.6, null, DimensionSource.USER_CONFIRMED, 1L)
        assertEquals(false, r.positionLocked)
        assertEquals(null, r.glueGroupId)
    }

    // ---------- članovi grupe ----------

    @Test
    fun aRoomWithoutAGroupIsAloneAndNotGlued() {
        val rooms = listOf(room("a", 0.0, 0.0), room("b", 4.2, 0.0))
        assertEquals(listOf("a"), RoomGroups.members(rooms, "a").map { it.id })
        assertTrue(!RoomGroups.isGlued(rooms, "a"))
    }

    @Test
    fun roomsWithTheSameGroupAreMembersOfEachOther() {
        val rooms = listOf(room("a", 0.0, 0.0, "g"), room("b", 4.2, 0.0, "g"), room("c", 9.0, 0.0))
        assertEquals(setOf("a", "b"), RoomGroups.members(rooms, "a").map { it.id }.toSet())
        assertTrue(RoomGroups.isGlued(rooms, "b"))
    }

    @Test
    fun aGroupOfOneRoomIsNotGlued() {
        val rooms = listOf(room("a", 0.0, 0.0, "g"))
        assertTrue(!RoomGroups.isGlued(rooms, "a"))
    }

    @Test
    fun theSameGroupIdOnAnotherFloorDoesNotGlueRooms() {
        val rooms = listOf(room("a", 0.0, 0.0, "g", level = 0), room("b", 0.0, 0.0, "g", level = 1))
        assertTrue(!RoomGroups.isGlued(rooms, "a"))
    }

    // ---------- pomeranje ----------

    @Test
    fun aSingleRoomMovesToTheNewPosition() {
        val rooms = listOf(room("a", 0.0, 0.0), room("b", 10.0, 0.0))
        val result = RoomGroups.planMove(rooms, "a", 1.0, 2.0) as RoomMoveResult.Moved
        assertEquals(mapOf("a" to PlanPoint(1.0, 2.0)), result.positions)
    }

    @Test
    fun gluedRoomsMoveByTheSameOffsetAndKeepTheirDistance() {
        val rooms = listOf(room("a", 0.0, 0.0, "g"), room("b", 4.2, 0.0, "g"))
        val result = RoomGroups.planMove(rooms, "a", 1.0, 2.0) as RoomMoveResult.Moved
        assertEquals(PlanPoint(1.0, 2.0), result.positions.getValue("a"))
        assertEquals(PlanPoint(5.2, 2.0), result.positions.getValue("b"))
    }

    @Test
    fun movingTheOtherMemberMovesTheWholeGroupToo() {
        val rooms = listOf(room("a", 0.0, 0.0, "g"), room("b", 4.2, 0.0, "g"))
        val result = RoomGroups.planMove(rooms, "b", 4.2, 1.0) as RoomMoveResult.Moved
        assertEquals(PlanPoint(0.0, 1.0), result.positions.getValue("a"))
        assertEquals(PlanPoint(4.2, 1.0), result.positions.getValue("b"))
    }

    @Test
    fun aLockedRoomCannotBeMoved() {
        val rooms = listOf(room("a", 0.0, 0.0, locked = true))
        val result = RoomGroups.planMove(rooms, "a", 1.0, 1.0)
        assertEquals(RoomMoveResult.Blocked("a"), result)
    }

    @Test
    fun aLockedRoomInTheGroupBlocksTheWholeGroup() {
        val rooms = listOf(room("a", 0.0, 0.0, "g"), room("b", 4.2, 0.0, "g", locked = true))
        assertEquals(RoomMoveResult.Blocked("b"), RoomGroups.planMove(rooms, "a", 1.0, 1.0))
    }

    @Test
    fun aRoomWithoutAPositionCannotBeMoved() {
        val rooms = listOf(room("a", null, null))
        assertEquals(RoomMoveResult.NotPlaced, RoomGroups.planMove(rooms, "a", 1.0, 1.0))
        assertEquals(RoomMoveResult.NotPlaced, RoomGroups.planMove(rooms, "missing", 1.0, 1.0))
    }

    // ---------- lepljenje ----------

    @Test
    fun touchingRoomsAreGluedIntoOneGroup() {
        val rooms = listOf(room("a", 0.0, 0.0), room("b", 4.2, 0.0), room("c", 20.0, 0.0))
        val result = RoomGroups.glueWithNeighbours(rooms, "a") { "novaGrupa" } as RoomGlueResult.Glued
        assertEquals("novaGrupa", result.groupId)
        assertEquals(mapOf("a" to "novaGrupa", "b" to "novaGrupa"), result.assignments)
        assertEquals(listOf("b"), result.neighbourIds)
    }

    @Test
    fun aRoomWithNoNeighbourCannotBeGlued() {
        val rooms = listOf(room("a", 0.0, 0.0), room("c", 20.0, 0.0))
        assertEquals(RoomGlueResult.NoNeighbours, RoomGroups.glueWithNeighbours(rooms, "a"))
    }

    @Test
    fun existingGroupsAreMergedIntoOne() {
        // a i b su već zalepljeni; c je pored b
        val rooms = listOf(
            room("a", 0.0, 0.0, "g1"), room("b", 4.2, 0.0, "g1"), room("c", 8.4, 0.0)
        )
        val result = RoomGroups.glueWithNeighbours(rooms, "c") { "ne" } as RoomGlueResult.Glued
        assertEquals("g1", result.groupId)
        assertEquals(setOf("a", "b", "c"), result.assignments.keys)
        assertTrue(result.assignments.values.all { it == "g1" })
    }

    @Test
    fun roomsOnAnotherFloorAreNeverNeighbours() {
        val rooms = listOf(room("a", 0.0, 0.0, level = 0), room("b", 4.2, 0.0, level = 1))
        assertEquals(RoomGlueResult.NoNeighbours, RoomGroups.glueWithNeighbours(rooms, "a"))
    }

    @Test
    fun overlappingRoomsAreNotNeighbours() {
        val rooms = listOf(room("a", 0.0, 0.0), room("b", 3.0, 0.0))
        assertEquals(RoomGlueResult.NoNeighbours, RoomGroups.glueWithNeighbours(rooms, "a"))
    }

    // ---------- odlepljivanje ----------

    @Test
    fun ungluingOneOfTwoRoomsSeparatesBoth() {
        val rooms = listOf(room("a", 0.0, 0.0, "g"), room("b", 4.2, 0.0, "g"))
        assertEquals(mapOf<String, String?>("a" to null, "b" to null), RoomGroups.unglue(rooms, "a"))
    }

    @Test
    fun ungluingOneRoomOfThreeLeavesTheOthersGlued() {
        val rooms = listOf(
            room("a", 0.0, 0.0, "g"), room("b", 4.2, 0.0, "g"), room("c", 8.4, 0.0, "g")
        )
        assertEquals(mapOf<String, String?>("b" to null), RoomGroups.unglue(rooms, "b"))
    }

    @Test
    fun ungluingARoomWithoutAGroupChangesNothing() {
        val rooms = listOf(room("a", 0.0, 0.0))
        assertTrue(RoomGroups.unglue(rooms, "a").isEmpty())
    }
}
