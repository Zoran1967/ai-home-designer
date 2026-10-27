package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomDrawingTest {

    @Test
    fun cornersAreSnappedToTheFiveCentimeterGrid() {
        val rect = RoomDrawing.rect(PlanPoint(1.02, 2.01), PlanPoint(5.23, 5.0))!!
        assertEquals(1.0, rect.xM, 1e-9)
        assertEquals(2.0, rect.yM, 1e-9)
        assertEquals(4.25, rect.lengthM, 1e-9)
        assertEquals(3.0, rect.widthM, 1e-9)
    }

    @Test
    fun theOrderOfTheTwoCornersDoesNotMatter() {
        val a = RoomDrawing.rect(PlanPoint(1.0, 2.0), PlanPoint(5.0, 5.0))
        val b = RoomDrawing.rect(PlanPoint(5.0, 5.0), PlanPoint(1.0, 2.0))
        val c = RoomDrawing.rect(PlanPoint(5.0, 2.0), PlanPoint(1.0, 5.0))
        assertEquals(a, b)
        assertEquals(a, c)
    }

    @Test
    fun aSideShorterThanTheMinimumIsRejected() {
        assertNull(RoomDrawing.rect(PlanPoint(0.0, 0.0), PlanPoint(0.4, 3.0)))
        assertNull(RoomDrawing.rect(PlanPoint(0.0, 0.0), PlanPoint(3.0, 0.45)))
        assertNull(RoomDrawing.rect(PlanPoint(2.0, 2.0), PlanPoint(2.0, 2.0)))
    }

    @Test
    fun aSideExactlyAtTheMinimumIsAccepted() {
        assertNotNull(RoomDrawing.rect(PlanPoint(0.0, 0.0), PlanPoint(0.5, 0.5)))
    }

    @Test
    fun negativeCoordinatesAreKept() {
        val rect = RoomDrawing.rect(PlanPoint(-2.0, -1.0), PlanPoint(1.0, 1.0))!!
        assertEquals(-2.0, rect.xM, 1e-9)
        assertEquals(-1.0, rect.yM, 1e-9)
        assertEquals(3.0, rect.lengthM, 1e-9)
        assertEquals(2.0, rect.widthM, 1e-9)
    }

    private val drawn = DrawnRect(0.0, 0.0, 4.0, 3.0)

    @Test
    fun noMismatchWhenTypedMeasuresAreCloseToTheImage() {
        assertTrue(RoomDrawing.mismatches(drawn, 4.0, 3.0).isEmpty())
        assertTrue(RoomDrawing.mismatches(drawn, 4.2, 2.8).isEmpty())
    }

    @Test
    fun exactlyTenPercentIsStillAccepted() {
        assertTrue(RoomDrawing.mismatches(drawn, 4.4, 2.7).isEmpty())
    }

    @Test
    fun moreThanTenPercentIsFlaggedPerAxis() {
        val result = RoomDrawing.mismatches(drawn, 5.5, 3.0)
        assertEquals(listOf(RoomDrawMismatch(DrawAxis.LENGTH, 4.0, 5.5)), result)
        val both = RoomDrawing.mismatches(drawn, 3.0, 4.5)
        assertEquals(2, both.size)
        assertEquals(DrawAxis.LENGTH, both[0].axis)
        assertEquals(DrawAxis.WIDTH, both[1].axis)
    }

    @Test
    fun aSmallerTypedMeasureIsFlaggedToo() {
        assertEquals(1, RoomDrawing.mismatches(drawn, 3.0, 3.0).size)
    }

    @Test
    fun measuresThatAreNotTypedYetAreNotCompared() {
        assertTrue(RoomDrawing.mismatches(drawn, null, null).isEmpty())
        assertEquals(1, RoomDrawing.mismatches(drawn, null, 5.0).size)
    }
}
