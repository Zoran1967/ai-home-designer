package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningValidatorTest {

    // Soba 4 m x 3 m, visine 2,6 m
    private val room = Room(
        id = "r1",
        projectId = "p",
        name = "Soba",
        lengthM = 4.0,
        widthM = 3.0,
        heightM = 2.6,
        wallThicknessM = null,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L,
        xM = 0.0,
        yM = 0.0
    )

    private fun door(
        side: WallSide = WallSide.NORTH,
        offset: String = "1",
        width: String = "0,9",
        height: String = "2,05",
        swing: DoorSwing? = DoorSwing.INTO_ROOM,
        hinge: HingeEnd? = HingeEnd.START
    ) = OpeningInput(OpeningType.DOOR, side, offset, width, height, "", swing, hinge)

    private fun window(
        side: WallSide = WallSide.EAST,
        offset: String = "0,5",
        width: String = "1,2",
        height: String = "1,2",
        sill: String = "0,9"
    ) = OpeningInput(OpeningType.WINDOW, side, offset, width, height, sill, null, null)

    private fun existing(
        id: String,
        side: WallSide,
        offset: Double,
        width: Double
    ) = Opening(
        id = id,
        roomId = "r1",
        type = OpeningType.DOOR,
        side = side,
        offsetM = offset,
        widthM = width,
        heightM = 2.0,
        sillHeightM = 0.0,
        swing = DoorSwing.INTO_ROOM,
        hinge = HingeEnd.START,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L
    )

    private fun errorsOf(
        input: OpeningInput,
        others: List<Opening> = emptyList(),
        editingId: String? = null
    ): OpeningInputErrors {
        val result = OpeningValidator.validate(input, room, others, editingId)
        assertTrue("Očekivana greška, a unos je prošao", result is OpeningValidation.Invalid)
        return (result as OpeningValidation.Invalid).errors
    }

    private fun validOf(
        input: OpeningInput,
        others: List<Opening> = emptyList(),
        editingId: String? = null
    ): OpeningValidation.Valid {
        val result = OpeningValidator.validate(input, room, others, editingId)
        assertTrue("Unos je trebalo da prođe: $result", result is OpeningValidation.Valid)
        return result as OpeningValidation.Valid
    }

    @Test
    fun wallLengthDependsOnSide() {
        assertEquals(4.0, WallGeometry.wallLengthM(room, WallSide.NORTH), 0.0)
        assertEquals(4.0, WallGeometry.wallLengthM(room, WallSide.SOUTH), 0.0)
        assertEquals(3.0, WallGeometry.wallLengthM(room, WallSide.EAST), 0.0)
        assertEquals(3.0, WallGeometry.wallLengthM(room, WallSide.WEST), 0.0)
    }

    @Test
    fun validDoorKeepsValuesAndHasNoSill() {
        val v = validOf(door())
        assertEquals(1.0, v.offsetM, 1e-9)
        assertEquals(0.9, v.widthM, 1e-9)
        assertEquals(2.05, v.heightM, 1e-9)
        assertEquals(0.0, v.sillHeightM, 0.0)
        assertEquals(DoorSwing.INTO_ROOM, v.swing)
        assertEquals(HingeEnd.START, v.hinge)
    }

    @Test
    fun slidingDoorHasNoHingeEvenIfOneWasGiven() {
        val v = validOf(door(swing = DoorSwing.SLIDING, hinge = HingeEnd.END))
        assertEquals(DoorSwing.SLIDING, v.swing)
        assertNull(v.hinge)
    }

    @Test
    fun slidingDoorDoesNotNeedAHinge() {
        validOf(door(swing = DoorSwing.SLIDING, hinge = null))
    }

    @Test
    fun doorWithoutSwingIsRejected() {
        val e = errorsOf(door(swing = null, hinge = null))
        assertTrue(e.swingMissing)
        assertFalse(e.hingeMissing)
    }

    @Test
    fun hingedDoorWithoutHingeEndIsRejected() {
        val e = errorsOf(door(swing = DoorSwing.OUT_OF_ROOM, hinge = null))
        assertTrue(e.hingeMissing)
        assertFalse(e.swingMissing)
    }

    @Test
    fun validWindowKeepsSillAndHasNoSwing() {
        val v = validOf(window())
        assertEquals(0.9, v.sillHeightM, 1e-9)
        assertNull(v.swing)
        assertNull(v.hinge)
    }

    @Test
    fun windowNeedsASillHeightButAZeroSillIsAllowed() {
        assertEquals(DimensionError.MISSING, errorsOf(window(sill = "")).sillHeight)
        assertEquals(0.0, validOf(window(sill = "0")).sillHeightM, 0.0)
    }

    @Test
    fun doorIgnoresTheSillField() {
        val input = door().copy(sillHeight = "abc")
        assertEquals(0.0, validOf(input).sillHeightM, 0.0)
    }

    @Test
    fun doorSizeLimitsAreChecked() {
        assertEquals(DimensionError.TOO_SMALL, errorsOf(door(width = "0,3")).width)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(door(width = "3,5")).width)
        assertEquals(DimensionError.TOO_SMALL, errorsOf(door(height = "1,2")).height)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(door(height = "3,5")).height)
    }

    @Test
    fun windowSizeLimitsAreChecked() {
        assertEquals(DimensionError.TOO_LARGE, errorsOf(window(width = "6")).width)
        assertEquals(DimensionError.TOO_SMALL, errorsOf(window(width = "0,2")).width)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(window(sill = "3")).sillHeight)
    }

    @Test
    fun narrowWindowIsFineEvenThoughTooNarrowForADoor() {
        validOf(window(width = "0,4"))
        assertEquals(DimensionError.TOO_SMALL, errorsOf(door(width = "0,4")).width)
    }

    @Test
    fun textAndEmptyFieldsAreRejected() {
        assertEquals(DimensionError.MISSING, errorsOf(door(offset = "")).offset)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(door(width = "abc")).width)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(door(height = "-2")).height)
    }

    @Test
    fun commaAndDotBothWork() {
        assertEquals(0.9, validOf(door(width = "0,9")).widthM, 1e-9)
        assertEquals(0.9, validOf(door(width = "0.9")).widthM, 1e-9)
    }

    @Test
    fun openingMustFitInTheWall() {
        assertEquals(OpeningPlacementError.EXCEEDS_WALL, errorsOf(door(offset = "3,5")).placement)
        // istočni zid je dug 3 m (širina sobe)
        assertEquals(
            OpeningPlacementError.EXCEEDS_WALL,
            errorsOf(door(side = WallSide.EAST, offset = "2,5")).placement
        )
    }

    @Test
    fun openingThatExactlyFillsTheWallFits() {
        validOf(door(offset = "3,1"))
        validOf(door(side = WallSide.WEST, offset = "2,1"))
    }

    @Test
    fun openingAtTheStartOfTheWallIsAllowed() {
        assertEquals(0.0, validOf(door(offset = "0")).offsetM, 0.0)
    }

    @Test
    fun openingTallerThanTheRoomIsRejected() {
        assertEquals(OpeningPlacementError.TOO_TALL_FOR_ROOM, errorsOf(door(height = "2,7")).placement)
    }

    @Test
    fun windowSillPlusHeightMustFitUnderTheCeiling() {
        val e = errorsOf(window(sill = "2", height = "1"))
        assertEquals(OpeningPlacementError.TOO_TALL_FOR_ROOM, e.placement)
        validOf(window(sill = "1,4", height = "1,2"))
    }

    @Test
    fun overlappingOpeningOnTheSameWallIsRejected() {
        val others = listOf(existing("d1", WallSide.NORTH, 1.0, 0.9))
        val e = errorsOf(window(side = WallSide.NORTH, offset = "1,5", width = "1"), others)
        assertEquals(OpeningPlacementError.OVERLAPS_OTHER, e.placement)
    }

    @Test
    fun openingsThatOnlyTouchAreAllowed() {
        val others = listOf(existing("d1", WallSide.NORTH, 1.0, 0.9))
        validOf(window(side = WallSide.NORTH, offset = "1,9", width = "1"), others)
    }

    @Test
    fun openingOnAnotherWallAtTheSameOffsetIsAllowed() {
        val others = listOf(existing("d1", WallSide.NORTH, 1.0, 0.9))
        validOf(door(side = WallSide.SOUTH, offset = "1"), others)
    }

    @Test
    fun editedOpeningIsNotComparedWithItself() {
        val others = listOf(existing("d1", WallSide.NORTH, 1.0, 0.9))
        validOf(door(offset = "1"), others, editingId = "d1")
    }

    @Test
    fun placementIsNotCheckedWhileAFieldIsStillInvalid() {
        val e = errorsOf(door(offset = "3,5", width = ""))
        assertEquals(DimensionError.MISSING, e.width)
        assertNull(e.placement)
    }

    @Test
    fun allProblemsAreReportedAtOnce() {
        val e = errorsOf(OpeningInput(OpeningType.DOOR, WallSide.NORTH, "", "abc", "9", "", null, null))
        assertEquals(DimensionError.MISSING, e.offset)
        assertEquals(DimensionError.NOT_A_NUMBER, e.width)
        assertEquals(DimensionError.TOO_LARGE, e.height)
        assertTrue(e.swingMissing)
    }
}
