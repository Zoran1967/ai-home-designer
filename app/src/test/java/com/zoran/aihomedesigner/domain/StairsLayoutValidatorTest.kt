package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StairsLayoutValidatorTest {

    private fun room(
        id: String, level: Int = 0, x: Double? = 0.0, y: Double? = 0.0,
        length: Double = 6.0, width: Double = 4.0
    ) = Room(
        id = id, projectId = "p", name = id, lengthM = length, widthM = width, heightM = 2.6,
        wallThicknessM = null, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = x, yM = y, floorLevel = level
    )

    // Penje se ka istoku: zauzimaju [x, x + 4] po x i [y, y + 1] po y
    private fun stairs(id: String = "s", x: Double = 1.0, y: Double = 1.0, lower: Int = 0) = Stairs(
        id = id, projectId = "p", lowerLevel = lower, xM = x, yM = y,
        lengthM = 4.0, widthM = 1.0, directionDeg = 0,
        dimensionSource = DimensionSource.USER_CONFIRMED, createdAtMillis = 1L
    )

    private fun issues(s: Stairs, rooms: List<Room>, all: List<Stairs> = listOf(s)) =
        StairsLayoutValidator.validate(s, all, rooms)

    @Test
    fun stairsInsideARoomHaveNoIssues() {
        assertTrue(issues(stairs(), listOf(room("a"))).isEmpty())
    }

    @Test
    fun stairsTouchingTheWallFromInsideAreAccepted() {
        // x od 2 do 6, soba je široka 6
        assertTrue(issues(stairs(x = 2.0), listOf(room("a"))).isEmpty())
    }

    @Test
    fun stairsPokingThroughTheWallAreFlagged() {
        // x od 3 do 7, a soba se završava na 6
        assertEquals(
            listOf(StairsIssue.NotInsideRoomBelow),
            issues(stairs(x = 3.0), listOf(room("a")))
        )
    }

    @Test
    fun stairsSpanningTwoRoomsAreFlagged() {
        val rooms = listOf(room("a", length = 3.0), room("b", x = 3.0, length = 3.0))
        assertEquals(
            listOf(StairsIssue.NotInsideRoomBelow),
            issues(stairs(x = 2.0), rooms)
        )
    }

    @Test
    fun aFloorWithoutRoomsIsNotChecked() {
        assertTrue(issues(stairs(), emptyList()).isEmpty())
        assertTrue(issues(stairs(lower = 3), listOf(room("a"))).isEmpty())
    }

    @Test
    fun roomsWithoutAPositionAreNotUsedForTheCheck() {
        assertTrue(issues(stairs(), listOf(room("a", x = null, y = null))).isEmpty())
    }

    @Test
    fun theUpperFloorMustAlsoContainTheStairsWhenItHasRooms() {
        val rooms = listOf(room("a", 0), room("b", 1, length = 3.0, width = 3.0))
        assertEquals(listOf(StairsIssue.NotInsideRoomAbove), issues(stairs(), rooms))
    }

    @Test
    fun anUpperRoomThatContainsTheStairsIsFine() {
        val rooms = listOf(room("a", 0), room("b", 1))
        assertTrue(issues(stairs(), rooms).isEmpty())
    }

    @Test
    fun bothProblemsCanBeReportedTogether() {
        val rooms = listOf(room("a", 0, length = 3.0), room("b", 1, length = 3.0))
        assertEquals(
            listOf(StairsIssue.NotInsideRoomBelow, StairsIssue.NotInsideRoomAbove),
            issues(stairs(), rooms)
        )
    }

    @Test
    fun overlappingStairsFromTheSameFloorAreFlagged() {
        val a = stairs("a", x = 1.0, y = 1.0)
        val b = stairs("b", x = 2.0, y = 1.5)
        assertEquals(listOf(StairsIssue.OverlapsStairs("b")), issues(a, listOf(room("r")), listOf(a, b)))
        assertEquals(listOf(StairsIssue.OverlapsStairs("a")), issues(b, listOf(room("r")), listOf(a, b)))
    }

    @Test
    fun stairsThatOnlyTouchAtAnEdgeDoNotOverlap() {
        val a = stairs("a", x = 1.0, y = 1.0) // y od 1 do 2
        val b = stairs("b", x = 1.0, y = 2.0) // y od 2 do 3
        assertTrue(issues(a, listOf(room("r")), listOf(a, b)).isEmpty())
    }

    @Test
    fun stairsStackedOnTopOfEachOtherOnDifferentFloorsAreNotOverlapping() {
        // 0 -> 1 i 1 -> 2 na istom mestu: nadovezuju se, nisu u sukobu
        val a = stairs("a", lower = 0)
        val b = stairs("b", lower = 1)
        assertTrue(issues(a, emptyList(), listOf(a, b)).isEmpty())
        assertTrue(issues(b, emptyList(), listOf(a, b)).isEmpty())
    }

    @Test
    fun stairsAreNotComparedWithThemselves() {
        val a = stairs("a")
        assertTrue(issues(a, emptyList(), listOf(a, a.copy())).isEmpty())
    }
}
