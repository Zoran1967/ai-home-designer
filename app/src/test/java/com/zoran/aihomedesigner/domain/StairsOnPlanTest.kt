package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class StairsOnPlanTest {

    private fun stairs(direction: Int, lowerLevel: Int = 0) = Stairs(
        id = "s", projectId = "p", lowerLevel = lowerLevel,
        xM = 1.0, yM = 2.0, lengthM = 4.0, widthM = 1.0,
        directionDeg = direction, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L
    )

    private fun assertPoint(expectedX: Double, expectedY: Double, actual: PlanPoint) {
        assertEquals(expectedX, actual.xM, 1e-9)
        assertEquals(expectedY, actual.yM, 1e-9)
    }

    @Test
    fun roleDependsOnTheFloorBeingShown() {
        val s = stairs(0, lowerLevel = 0)
        assertEquals(StairsRole.DEPARTS, StairsOnPlan.role(s, 0))
        assertEquals(StairsRole.ARRIVES, StairsOnPlan.role(s, 1))
        assertNull(StairsOnPlan.role(s, 2))
        assertNull(StairsOnPlan.role(s, -1))
    }

    @Test
    fun roleWorksForBasementStairs() {
        val s = stairs(0, lowerLevel = -1)
        assertEquals(StairsRole.DEPARTS, StairsOnPlan.role(s, -1))
        assertEquals(StairsRole.ARRIVES, StairsOnPlan.role(s, 0))
    }

    @Test
    fun stepLinesAreSpacedEvenlyAcrossTheRunEastWards() {
        // 5 stepenika = 4 gazišta = 3 unutrašnje crte; krak je dug 4 m, počinje na x = 1
        val lines = StairsOnPlan.stepLines(stairs(0), 5)
        assertEquals(3, lines.size)
        assertPoint(2.0, 2.0, lines[0].from)
        assertPoint(2.0, 3.0, lines[0].to)
        assertPoint(3.0, 2.0, lines[1].from)
        assertPoint(4.0, 3.0, lines[2].to)
    }

    @Test
    fun stepLinesAreHorizontalWhenClimbingSouth() {
        // Pravac 90: krak ide duž y (od y = 2 do y = 6), a širina 1 m duž x
        val lines = StairsOnPlan.stepLines(stairs(90), 3)
        assertEquals(1, lines.size)
        assertPoint(1.0, 4.0, lines[0].from)
        assertPoint(2.0, 4.0, lines[0].to)
    }

    @Test
    fun twoStepsHaveNoInnerLines() {
        assertEquals(0, StairsOnPlan.stepLines(stairs(0), 2).size)
    }

    @Test
    fun stepLinesNeedAtLeastTwoSteps() {
        assertThrows(IllegalArgumentException::class.java) { StairsOnPlan.stepLines(stairs(0), 1) }
    }

    @Test
    fun arrowPointsInTheClimbingDirection() {
        val east = StairsOnPlan.climbArrow(stairs(0))
        assertPoint(1.0, 2.5, east.from)
        assertPoint(5.0, 2.5, east.to)

        val west = StairsOnPlan.climbArrow(stairs(180))
        assertPoint(5.0, 2.5, west.from)
        assertPoint(1.0, 2.5, west.to)

        val south = StairsOnPlan.climbArrow(stairs(90))
        assertPoint(1.5, 2.0, south.from)
        assertPoint(1.5, 6.0, south.to)

        val north = StairsOnPlan.climbArrow(stairs(270))
        assertPoint(1.5, 6.0, north.from)
        assertPoint(1.5, 2.0, north.to)
    }

    @Test
    fun arrowRejectsAnUnknownDirection() {
        assertThrows(IllegalArgumentException::class.java) { StairsOnPlan.climbArrow(stairs(45)) }
    }
}
