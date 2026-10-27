package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomMeasureEditTest {

    private fun room(
        x: Double? = 1.0, y: Double? = 2.0, length: Double = 4.0, width: Double = 3.0,
        height: Double = 2.6, thickness: Double? = null
    ) = Room(
        id = "r", projectId = "p", name = "Dnevna", lengthM = length, widthM = width, heightM = height,
        wallThicknessM = thickness, dimensionSource = DimensionSource.IMPORTED_CALIBRATED,
        createdAtMillis = 1L, xM = x, yM = y, floorLevel = 0
    )

    @Test
    fun anchorsAreOnTheMiddleOfTheTopLeftAndBottomEdges() {
        val anchors = RoomMeasureEdit.anchors(room())
        assertEquals(
            listOf(
                MeasureAnchor(MeasureKind.LENGTH, 3.0, 2.0),
                MeasureAnchor(MeasureKind.WIDTH, 1.0, 3.5),
                MeasureAnchor(MeasureKind.HEIGHT, 3.0, 5.0)
            ),
            anchors
        )
    }

    @Test
    fun aRoomWithoutAPositionHasNoAnchors() {
        assertTrue(RoomMeasureEdit.anchors(room(x = null, y = null)).isEmpty())
        assertTrue(RoomMeasureEdit.anchors(room(x = 1.0, y = null)).isEmpty())
    }

    @Test
    fun valueOfReturnsTheMatchingMeasure() {
        val r = room()
        assertEquals(4.0, RoomMeasureEdit.valueOf(r, MeasureKind.LENGTH), 0.0)
        assertEquals(3.0, RoomMeasureEdit.valueOf(r, MeasureKind.WIDTH), 0.0)
        assertEquals(2.6, RoomMeasureEdit.valueOf(r, MeasureKind.HEIGHT), 0.0)
    }

    @Test
    fun limitsMatchTheRoomForm() {
        assertEquals(0.5 to 50.0, RoomMeasureEdit.limits(MeasureKind.LENGTH))
        assertEquals(0.5 to 50.0, RoomMeasureEdit.limits(MeasureKind.WIDTH))
        assertEquals(1.8 to 6.0, RoomMeasureEdit.limits(MeasureKind.HEIGHT))
    }

    @Test
    fun onlyTheTouchedMeasureIsReplaced() {
        val r = room(thickness = 0.2)
        val input = RoomMeasureEdit.input(r, MeasureKind.WIDTH, "3,5")
        assertEquals("Dnevna", input.name)
        assertEquals("4", input.length)
        assertEquals("3,5", input.width)
        assertEquals("2,6", input.height)
        assertEquals("0,2", input.wallThickness)
    }

    @Test
    fun aMissingWallThicknessStaysEmpty() {
        assertEquals("", RoomMeasureEdit.input(room(thickness = null), MeasureKind.LENGTH, "5").wallThickness)
    }

    @Test
    fun untouchedMeasuresComeBackAsExactlyTheSameNumbers() {
        // Neizmenjene mere ne smeju da se pomere ni za hiljaditi deo pri pretvaranju u tekst i nazad
        val r = room(length = 4.2, width = 3.3, height = 2.55, thickness = 0.125)
        val valid = RoomValidator.validate(RoomMeasureEdit.input(r, MeasureKind.LENGTH, "4,2"), emptyList())
            as RoomValidation.Valid
        assertEquals(4.2, valid.lengthM, 0.0)
        assertEquals(3.3, valid.widthM, 0.0)
        assertEquals(2.55, valid.heightM, 0.0)
        assertEquals(0.125, valid.wallThicknessM!!, 0.0)
    }

    @Test
    fun theNewValueGoesThroughTheNormalValidation() {
        val r = room()
        val tooTall = RoomValidator.validate(RoomMeasureEdit.input(r, MeasureKind.HEIGHT, "7"), emptyList())
            as RoomValidation.Invalid
        assertEquals(DimensionError.TOO_LARGE, tooTall.errors.height)
        assertEquals(null, tooTall.errors.length)
        val notANumber = RoomValidator.validate(RoomMeasureEdit.input(r, MeasureKind.LENGTH, "abc"), emptyList())
            as RoomValidation.Invalid
        assertEquals(DimensionError.NOT_A_NUMBER, notANumber.errors.length)
    }

    @Test
    fun inputTextUsesACommaAndNoTrailingZeros() {
        assertEquals("4", RoomMeasureEdit.inputText(4.0))
        assertEquals("4,25", RoomMeasureEdit.inputText(4.25))
        assertEquals("0,5", RoomMeasureEdit.inputText(0.5))
    }
}
