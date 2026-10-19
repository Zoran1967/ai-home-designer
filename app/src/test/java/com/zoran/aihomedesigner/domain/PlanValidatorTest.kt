package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanValidatorTest {

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
    fun noRoomsMeansNoIssues() {
        assertTrue(PlanValidator.validate(emptyList()).isEmpty())
    }

    @Test
    fun farApartRoomsHaveNoIssues() {
        val issues = PlanValidator.validate(listOf(room("a", 0.0, 0.0), room("b", 20.0, 0.0)))
        assertTrue(issues.isEmpty())
    }

    @Test
    fun overlappingRoomsAreReported() {
        val issues = PlanValidator.validate(listOf(room("a", 0.0, 0.0), room("b", 3.0, 1.0)))
        assertEquals(1, issues.size)
        val issue = issues[0] as PlanIssue.RoomsOverlap
        assertEquals(setOf("a", "b"), setOf(issue.roomAId, issue.roomBId))
    }

    @Test
    fun touchingRoomsWithoutWallAreReported() {
        val issues = PlanValidator.validate(listOf(room("a", 0.0, 0.0), room("b", 4.0, 0.0)))
        assertEquals(1, issues.size)
        assertTrue(issues[0] is PlanIssue.WallTooThin)
    }

    @Test
    fun veryThinGapIsReported() {
        val issues = PlanValidator.validate(listOf(room("a", 0.0, 0.0), room("b", 4.02, 0.0)))
        assertEquals(1, issues.size)
        assertTrue(issues[0] is PlanIssue.WallTooThin)
    }

    @Test
    fun wallOfMinimumThicknessIsAccepted() {
        val issues = PlanValidator.validate(listOf(room("a", 0.0, 0.0), room("b", 4.05, 0.0)))
        assertTrue(issues.isEmpty())
    }

    @Test
    fun wallMatchingEnteredThicknessHasNoIssues() {
        val rooms = listOf(
            room("a", 0.0, 0.0, thickness = 0.2),
            room("b", 4.2, 0.0, thickness = 0.2)
        )
        assertTrue(PlanValidator.validate(rooms).isEmpty())
    }

    @Test
    fun wallThinnerThanEnteredThicknessIsReportedForThatRoom() {
        val rooms = listOf(
            room("a", 0.0, 0.0, thickness = 0.25),
            room("b", 4.2, 0.0, thickness = 0.2)
        )
        val issues = PlanValidator.validate(rooms)
        assertEquals(1, issues.size)
        val issue = issues[0] as PlanIssue.WallThinnerThanEntered
        assertEquals("a", issue.roomId)
        assertEquals(0.25, issue.enteredThicknessM, 1e-9)
        assertEquals(0.2, issue.wall.thicknessM, 1e-9)
    }

    @Test
    fun smallDifferenceWithinToleranceIsAccepted() {
        val rooms = listOf(
            room("a", 0.0, 0.0, thickness = 0.2),
            room("b", 4.195, 0.0)
        )
        assertTrue(PlanValidator.validate(rooms).isEmpty())
    }

    @Test
    fun missingThicknessIsNotAnError() {
        val rooms = listOf(room("a", 0.0, 0.0), room("b", 4.2, 0.0))
        assertTrue(PlanValidator.validate(rooms).isEmpty())
    }

    @Test
    fun thickerWallThanEnteredIsAccepted() {
        val rooms = listOf(
            room("a", 0.0, 0.0, thickness = 0.1),
            room("b", 4.3, 0.0, thickness = 0.1)
        )
        assertTrue(PlanValidator.validate(rooms).isEmpty())
    }

    @Test
    fun unplacedRoomsAreIgnored() {
        val issues = PlanValidator.validate(listOf(room("a", 0.0, 0.0), room("b", null, null)))
        assertTrue(issues.isEmpty())
    }
}
