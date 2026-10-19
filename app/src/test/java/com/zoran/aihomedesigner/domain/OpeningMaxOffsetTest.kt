package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningMaxOffsetTest {

    private fun room(length: Double = 4.0, width: Double = 3.0) = Room(
        id = "r1",
        projectId = "p",
        name = "Soba",
        lengthM = length,
        widthM = width,
        heightM = 2.6,
        wallThicknessM = null,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L,
        xM = 0.0,
        yM = 0.0
    )

    private fun door(offset: String, width: String = "0,9") = OpeningInput(
        OpeningType.DOOR, WallSide.NORTH, offset, width, "2,05", "", DoorSwing.INTO_ROOM, HingeEnd.START
    )

    private fun errors(input: OpeningInput, room: Room): OpeningInputErrors {
        val result = OpeningValidator.validate(input, room, emptyList())
        assertTrue(result is OpeningValidation.Invalid)
        return (result as OpeningValidation.Invalid).errors
    }

    @Test
    fun errorTellsTheLargestOffsetThatWouldFit() {
        val e = errors(door("3,5"), room(length = 4.0))
        assertEquals(OpeningPlacementError.EXCEEDS_WALL, e.placement)
        assertEquals(3.1, e.maxOffsetM!!, 1e-9)
    }

    @Test
    fun wallOfOnePointThreeMetresAllowsADoorOfPointNineOnlyUpToPointFour() {
        // Soba duga 1,3 m: severni zid je dug 1,3 m
        val tooFar = errors(door("0,5"), room(length = 1.3))
        assertEquals(0.4, tooFar.maxOffsetM!!, 1e-9)

        val result = OpeningValidator.validate(door("0,4"), room(length = 1.3), emptyList())
        assertTrue(result is OpeningValidation.Valid)
    }

    @Test
    fun maxOffsetIsNegativeWhenTheOpeningIsWiderThanTheWholeWall() {
        val e = errors(door("0", width = "0,9"), room(length = 0.5))
        assertEquals(OpeningPlacementError.EXCEEDS_WALL, e.placement)
        assertTrue(e.maxOffsetM!! < 0.0)
    }

    @Test
    fun maxOffsetIsNotSetForOtherProblems() {
        val e = errors(door("1", width = "0,9").copy(height = "2,9"), room())
        assertEquals(OpeningPlacementError.TOO_TALL_FOR_ROOM, e.placement)
        assertNull(e.maxOffsetM)
    }

    @Test
    fun centeredOffsetPutsTheOpeningInTheMiddle() {
        assertEquals(0.2, OpeningValidator.centeredOffsetM(1.3, 0.9)!!, 1e-9)
        assertEquals(1.55, OpeningValidator.centeredOffsetM(4.0, 0.9)!!, 1e-9)
        assertEquals(0.0, OpeningValidator.centeredOffsetM(0.9, 0.9)!!, 1e-9)
    }

    @Test
    fun centeredOffsetIsNullWhenTheOpeningIsWiderThanTheWall() {
        assertNull(OpeningValidator.centeredOffsetM(0.5, 0.9))
    }

    @Test
    fun centeredOpeningAlwaysPassesThePlacementCheck() {
        val offset = OpeningValidator.centeredOffsetM(1.3, 0.9)!!
        val text = offset.toString().replace('.', ',')
        val result = OpeningValidator.validate(door(text), room(length = 1.3), emptyList())
        assertTrue("Sredina zida je trebalo da prođe: $result", result is OpeningValidation.Valid)
    }

    @Test
    fun parseMetersAcceptsCommaAndDotAndRejectsJunk() {
        assertEquals(0.9, OpeningValidator.parseMeters("0,9")!!, 1e-9)
        assertEquals(0.9, OpeningValidator.parseMeters(" 0.9 ")!!, 1e-9)
        assertNull(OpeningValidator.parseMeters(""))
        assertNull(OpeningValidator.parseMeters("abc"))
        assertNull(OpeningValidator.parseMeters("-1"))
        assertNotNull(OpeningValidator.parseMeters("12"))
    }
}
