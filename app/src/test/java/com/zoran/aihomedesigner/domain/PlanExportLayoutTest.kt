package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanExportLayoutTest {

    private fun room(id: String, x: Double?, y: Double?, thickness: Double? = 0.2) = Room(
        id = id, projectId = "p", name = id,
        lengthM = 4.0, widthM = 3.0, heightM = 2.6,
        wallThicknessM = thickness, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = x, yM = y
    )

    @Test
    fun boundsIncludeTheWalls() {
        val b = PlanExportLayout.bounds(listOf(room("a", 2.0, 1.0)))!!
        assertEquals(1.8, b.x0, 1e-9)
        assertEquals(0.8, b.y0, 1e-9)
        assertEquals(6.2, b.x1, 1e-9)
        assertEquals(4.2, b.y1, 1e-9)
    }

    @Test
    fun boundsCoverAllPlacedRoomsAndIgnoreUnplacedOnes() {
        val b = PlanExportLayout.bounds(
            listOf(room("a", 0.0, 0.0), room("b", 10.0, 5.0), room("c", null, null))
        )!!
        assertEquals(-0.2, b.x0, 1e-9)
        assertEquals(14.2, b.x1, 1e-9)
        assertEquals(8.2, b.y1, 1e-9)
    }

    @Test
    fun missingWallThicknessUsesTheDisplayDefault() {
        val b = PlanExportLayout.bounds(listOf(room("a", 0.0, 0.0, thickness = null)))!!
        assertEquals(-SceneBuilder.VISUAL_WALL_M, b.x0, 1e-9)
    }

    @Test
    fun noPlacedRoomsMeansNoBounds() {
        assertNull(PlanExportLayout.bounds(emptyList()))
        assertNull(PlanExportLayout.bounds(listOf(room("a", null, null))))
    }

    @Test
    fun wideDrawingIsLimitedByThePageWidthAndCentredVertically() {
        val frame = PlanExportLayout.frame(
            PlanRect(0.0, 0.0, 10.0, 5.0),
            pageWidth = 1000.0, pageHeight = 800.0,
            margin = 50.0, topReserved = 0.0, bottomReserved = 0.0
        )
        assertEquals(90.0, frame.scale, 1e-9)
        assertEquals(50.0, frame.offsetX, 1e-9)
        assertEquals(175.0, frame.offsetY, 1e-9)
    }

    @Test
    fun tallDrawingIsLimitedByThePageHeight() {
        val frame = PlanExportLayout.frame(
            PlanRect(0.0, 0.0, 2.0, 10.0),
            pageWidth = 1000.0, pageHeight = 800.0,
            margin = 50.0, topReserved = 0.0, bottomReserved = 0.0
        )
        assertEquals(70.0, frame.scale, 1e-9)
    }

    @Test
    fun reservedSpaceForTitleAndLegendIsKeptFree() {
        val bounds = PlanRect(0.0, 0.0, 10.0, 10.0)
        val frame = PlanExportLayout.frame(bounds, 1000.0, 1000.0, margin = 50.0, topReserved = 100.0, bottomReserved = 100.0)
        val top = frame.y(bounds.y0)
        val bottom = frame.y(bounds.y1)
        assertTrue(top >= 50.0 + 100.0 - 1e-9)
        assertTrue(bottom <= 1000.0 - 50.0 - 100.0 + 1e-9)
        val left = frame.x(bounds.x0)
        val right = frame.x(bounds.x1)
        assertTrue(left >= 50.0 - 1e-9)
        assertTrue(right <= 950.0 + 1e-9)
    }

    @Test
    fun theDrawingNeverLeavesThePageForOddShapes() {
        val bounds = PlanRect(-3.0, 7.0, 21.0, 12.5)
        val frame = PlanExportLayout.frame(bounds, 595.0, 842.0, margin = 36.0, topReserved = 60.0, bottomReserved = 80.0)
        assertTrue(frame.x(bounds.x0) >= 36.0 - 1e-9)
        assertTrue(frame.x(bounds.x1) <= 595.0 - 36.0 + 1e-9)
        assertTrue(frame.y(bounds.y0) >= 36.0 + 60.0 - 1e-9)
        assertTrue(frame.y(bounds.y1) <= 842.0 - 36.0 - 80.0 + 1e-9)
    }

    @Test
    fun scaleBarIsTheLargestNiceLengthThatFits() {
        assertEquals(2.0, PlanExportLayout.scaleBarMeters(90.0, 180.0), 0.0)
        assertEquals(1.0, PlanExportLayout.scaleBarMeters(90.0, 100.0), 0.0)
        assertEquals(10.0, PlanExportLayout.scaleBarMeters(30.0, 400.0), 0.0)
    }

    @Test
    fun scaleBarFallsBackToTheSmallestLengthWhenNothingFits() {
        assertEquals(0.1, PlanExportLayout.scaleBarMeters(1000.0, 50.0), 0.0)
    }

    @Test
    fun scaleMustBePositive() {
        assertThrows(IllegalArgumentException::class.java) { PlanExportLayout.scaleBarMeters(0.0, 100.0) }
    }
}
