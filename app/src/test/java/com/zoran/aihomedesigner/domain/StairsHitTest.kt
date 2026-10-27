package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StairsHitTest {

    // Duge 4 m, široke 1 m, penju se ka istoku: zauzimaju x od 1 do 5 i y od 2 do 3
    private fun stairs(direction: Int) = Stairs(
        id = "s", projectId = "p", lowerLevel = 0,
        xM = 1.0, yM = 2.0, lengthM = 4.0, widthM = 1.0,
        directionDeg = direction, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L
    )

    @Test
    fun pointInsideTheFootprintIsAHit() {
        assertTrue(StairsOnPlan.contains(stairs(0), 3.0, 2.5))
    }

    @Test
    fun pointsOnTheEdgeAndCornersAreAHit() {
        assertTrue(StairsOnPlan.contains(stairs(0), 1.0, 2.0))
        assertTrue(StairsOnPlan.contains(stairs(0), 5.0, 3.0))
    }

    @Test
    fun pointOutsideTheFootprintIsNotAHit() {
        assertFalse(StairsOnPlan.contains(stairs(0), 0.99, 2.5))
        assertFalse(StairsOnPlan.contains(stairs(0), 5.01, 2.5))
        assertFalse(StairsOnPlan.contains(stairs(0), 3.0, 1.99))
        assertFalse(StairsOnPlan.contains(stairs(0), 3.0, 3.01))
    }

    @Test
    fun goingSouthTheFootprintIsTallAndNarrow() {
        // Pravac 90: zauzimaju x od 1 do 2 i y od 2 do 6
        assertTrue(StairsOnPlan.contains(stairs(90), 1.5, 5.5))
        assertFalse(StairsOnPlan.contains(stairs(90), 3.0, 2.5))
        assertFalse(StairsOnPlan.contains(stairs(90), 1.5, 6.1))
    }

    @Test
    fun oppositeDirectionsOccupyTheSameArea() {
        assertTrue(StairsOnPlan.contains(stairs(180), 4.5, 2.5))
        assertTrue(StairsOnPlan.contains(stairs(270), 1.5, 5.0))
    }
}
