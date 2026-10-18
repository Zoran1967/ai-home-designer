package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningValidatorTest {

    // Soba 4,0 m (dužina) x 3,0 m (širina), visina zida 2,6 m.
    private fun room(height: Double = 2.6) = Room(
        id = "room1",
        projectId = "p",
        name = "Soba",
        lengthM = 4.0,
        widthM = 3.0,
        heightM = height,
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
        height: String = "2,05"
    ) = OpeningInput(
        side, OpeningType.DOOR, offset, width, height, "", DoorSwing.INTO_ROOM, HingeSide.START
    )

    private fun window(
        side: WallSide = WallSide.NORTH,
        offset: String = "1",
        width: String = "1,2",
        height: String = "1,2",
        sill: String = "0,9"
    ) = OpeningInput(
        side, OpeningType.WINDOW, offset, width, height, sill, DoorSwing.INTO_ROOM, HingeSide.START
    )

    private fun existing(side: WallSide, offset: Double, width: Double, roomId: String = "room1") = Opening(
        id = "o-$offset", roomId = roomId, side = side, type = OpeningType.WINDOW,
        offsetM = offset, widthM = width, heightM = 1.2, sillHeightM = 0.9,
        swing = null, hinge = null, dimensionSource = DimensionSource.USER_CONFIRMED, createdAtMillis = 1L
    )

    private fun errorsOf(
        input: OpeningInput,
        room: Room = room(),
        others: List<Opening> = emptyList()
    ): OpeningInputErrors {
        val result = OpeningValidator.validate(input, room, others)
        assertTrue("Očekivana greška, a unos je prošao", result is OpeningValidation.Invalid)
        return (result as OpeningValidation.Invalid).errors
    }

    private fun validOf(
        input: OpeningInput,
        room: Room = room(),
        others: List<Opening> = emptyList()
    ): OpeningValidation.Valid {
        val result = OpeningValidator.validate(input, room, others)
        assertTrue("Unos je trebalo da prođe", result is OpeningValidation.Valid)
        return result as OpeningValidation.Valid
    }

    @Test
    fun validDoorKeepsValuesAndHasZeroSill() {
        val v = validOf(door())
        assertEquals(1.0, v.offsetM, 1e-9)
        assertEquals(0.9, v.widthM, 1e-9)
        assertEquals(2.05, v.heightM, 1e-9)
        assertEquals(0.0, v.sillHeightM, 1e-9)
        assertEquals(DoorSwing.INTO_ROOM, v.swing)
        assertEquals(HingeSide.START, v.hinge)
    }

    @Test
    fun validWindowKeepsSillAndHasNoSwing() {
        val v = validOf(window())
        assertEquals(0.9, v.sillHeightM, 1e-9)
        assertNull(v.swing)
        assertNull(v.hinge)
    }

    @Test
    fun doorIgnoresSillText() {
        val input = door().copy(sillHeight = "abc")
        assertEquals(0.0, validOf(input).sillHeightM, 1e-9)
    }

    @Test
    fun windowNeedsSill() {
        assertEquals(DimensionError.MISSING, errorsOf(window(sill = "")).sillHeight)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(window(sill = "3")).sillHeight)
    }

    @Test
    fun windowOnTheFloorIsAllowed() {
        assertEquals(0.0, validOf(window(sill = "0")).sillHeightM, 1e-9)
    }

    @Test
    fun commaAndDotBothWork() {
        assertEquals(1.5, validOf(door(offset = "1,5")).offsetM, 1e-9)
        assertEquals(1.5, validOf(door(offset = "1.5")).offsetM, 1e-9)
    }

    @Test
    fun textIsNotANumberAndEmptyIsMissing() {
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(door(offset = "abc")).offset)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(door(width = "-1")).width)
        assertEquals(DimensionError.MISSING, errorsOf(door(offset = "")).offset)
        assertEquals(DimensionError.MISSING, errorsOf(door(height = " ")).height)
    }

    @Test
    fun doorSizeLimitsAreChecked() {
        assertEquals(DimensionError.TOO_SMALL, errorsOf(door(width = "0,3")).width)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(door(width = "4")).width)
        assertEquals(DimensionError.TOO_SMALL, errorsOf(door(height = "1,5")).height)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(door(height = "3,5")).height)
    }

    @Test
    fun windowCanBeSmallerThanADoor() {
        validOf(window(width = "0,4", height = "0,4"))
    }

    @Test
    fun openingMustFitInTheWall() {
        val errors = errorsOf(door(side = WallSide.NORTH, offset = "3,5"))
        assertEquals(OpeningLayoutError.OUT_OF_WALL, errors.layout)
    }

    @Test
    fun wallLengthDependsOnTheSide() {
        // Severni zid je dug 4,0 m, a zapadni 3,0 m: isti otvor staje na jednom, a na drugom ne.
        validOf(door(side = WallSide.NORTH, offset = "2,5"))
        val errors = errorsOf(door(side = WallSide.WEST, offset = "2,5"))
        assertEquals(OpeningLayoutError.OUT_OF_WALL, errors.layout)
        validOf(door(side = WallSide.SOUTH, offset = "2,5"))
        assertEquals(
            OpeningLayoutError.OUT_OF_WALL,
            errorsOf(door(side = WallSide.EAST, offset = "2,5")).layout
        )
    }

    @Test
    fun openingThatExactlyFillsTheWallIsAccepted() {
        validOf(door(offset = "3,1", width = "0,9"))
        validOf(door(offset = "0", width = "3"))
    }

    @Test
    fun openingMustFitInRoomHeight() {
        val lowRoom = room(height = 2.0)
        val errors = errorsOf(door(height = "2,1"), room = lowRoom)
        assertEquals(OpeningLayoutError.TOO_HIGH_FOR_ROOM, errors.layout)
    }

    @Test
    fun windowSillPlusHeightMustFitInRoomHeight() {
        val errors = errorsOf(window(height = "1,2", sill = "1,5"))
        assertEquals(OpeningLayoutError.TOO_HIGH_FOR_ROOM, errors.layout)
        validOf(window(height = "1,2", sill = "1,4"))
    }

    @Test
    fun overlapWithAnotherOpeningOnTheSameWallIsRejected() {
        val others = listOf(existing(WallSide.NORTH, offset = 1.0, width = 1.0))
        val errors = errorsOf(window(offset = "1,5"), others = others)
        assertEquals(OpeningLayoutError.OVERLAPS_OTHER, errors.layout)
    }

    @Test
    fun openingContainingAnotherOneOverlaps() {
        val others = listOf(existing(WallSide.NORTH, offset = 1.5, width = 0.5))
        val errors = errorsOf(window(offset = "1", width = "2"), others = others)
        assertEquals(OpeningLayoutError.OVERLAPS_OTHER, errors.layout)
    }

    @Test
    fun openingsThatOnlyTouchDoNotOverlap() {
        val others = listOf(existing(WallSide.NORTH, offset = 1.0, width = 1.0))
        validOf(window(offset = "2"), others = others)
        validOf(window(offset = "0", width = "1"), others = others)
    }

    @Test
    fun sameOffsetOnAnotherWallIsFine() {
        val others = listOf(existing(WallSide.NORTH, offset = 1.0, width = 1.0))
        validOf(window(side = WallSide.SOUTH, offset = "1"), others = others)
    }

    @Test
    fun openingsOfOtherRoomsAreIgnored() {
        val others = listOf(existing(WallSide.NORTH, offset = 1.0, width = 1.0, roomId = "other"))
        validOf(window(offset = "1"), others = others)
    }

    @Test
    fun fieldErrorsAreReportedBeforeLayoutErrors() {
        val errors = errorsOf(door(offset = "abc", width = "9"))
        assertEquals(DimensionError.NOT_A_NUMBER, errors.offset)
        assertEquals(DimensionError.TOO_LARGE, errors.width)
        assertNull(errors.layout)
    }

    @Test
    fun layoutErrorCheckWorksForAlreadySavedOpenings() {
        val smallRoom = room().copy(lengthM = 3.0)
        val error = OpeningValidator.layoutError(
            WallSide.NORTH, offsetM = 2.5, widthM = 0.9, heightM = 2.0, sillHeightM = 0.0,
            room = smallRoom, others = emptyList()
        )
        assertEquals(OpeningLayoutError.OUT_OF_WALL, error)
    }

    @Test
    fun endOfOpeningIsOffsetPlusWidth() {
        assertEquals(2.0, existing(WallSide.NORTH, 1.0, 1.0).endM, 1e-9)
    }
}
