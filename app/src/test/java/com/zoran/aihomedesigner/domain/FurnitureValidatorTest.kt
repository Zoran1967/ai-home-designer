package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FurnitureValidatorTest {

    // Soba 4 m x 3 m
    private val room = Room(
        id = "r",
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

    private fun input(
        name: String = "Krevet",
        category: FurnitureCategory? = FurnitureCategory.BED,
        length: String = "2",
        width: String = "1,6",
        height: String = "0,5"
    ) = FurnitureInput(name, category, length, width, height)

    private fun errorsOf(input: FurnitureInput): FurnitureInputErrors {
        val result = FurnitureValidator.validate(input, room)
        assertTrue("Očekivana greška, a unos je prošao", result is FurnitureValidation.Invalid)
        return (result as FurnitureValidation.Invalid).errors
    }

    private fun validOf(input: FurnitureInput): FurnitureValidation.Valid {
        val result = FurnitureValidator.validate(input, room)
        assertTrue("Unos je trebalo da prođe: $result", result is FurnitureValidation.Valid)
        return result as FurnitureValidation.Valid
    }

    @Test
    fun validInputKeepsValues() {
        val v = validOf(input())
        assertEquals("Krevet", v.name)
        assertEquals(FurnitureCategory.BED, v.category)
        assertEquals(2.0, v.lengthM, 1e-9)
        assertEquals(1.6, v.widthM, 1e-9)
        assertEquals(0.5, v.heightM, 1e-9)
    }

    @Test
    fun nameIsNormalized() {
        assertEquals("Radni sto", validOf(input(name = "  Radni    sto ")).name)
    }

    @Test
    fun emptyAndTooLongNamesAreRejected() {
        assertEquals(FurnitureNameError.EMPTY, errorsOf(input(name = "  ")).name)
        val tooLong = "a".repeat(FurnitureLimits.MAX_NAME_LENGTH + 1)
        assertEquals(FurnitureNameError.TOO_LONG, errorsOf(input(name = tooLong)).name)
        validOf(input(name = "a".repeat(FurnitureLimits.MAX_NAME_LENGTH)))
    }

    @Test
    fun sameNameTwiceIsAllowedBecauseYouCanHaveManyChairs() {
        validOf(input(name = "Stolica"))
        validOf(input(name = "Stolica"))
    }

    @Test
    fun categoryIsRequired() {
        assertTrue(errorsOf(input(category = null)).categoryMissing)
    }

    @Test
    fun dimensionsAreChecked() {
        assertEquals(DimensionError.MISSING, errorsOf(input(length = "")).length)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(input(width = "abc")).width)
        assertEquals(DimensionError.TOO_SMALL, errorsOf(input(height = "0,05")).height)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(input(height = "3,5")).height)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(input(length = "6")).length)
    }

    @Test
    fun commaAndDotBothWork() {
        assertEquals(1.6, validOf(input(width = "1,6")).widthM, 1e-9)
        assertEquals(1.6, validOf(input(width = "1.6")).widthM, 1e-9)
    }

    @Test
    fun objectBiggerThanTheRoomIsRejected() {
        val e = errorsOf(input(length = "4,5", width = "3,5"))
        assertTrue(e.tooBigForRoom)
    }

    @Test
    fun objectThatFitsOnlyWhenTurnedIsAccepted() {
        // 3 x 2 staje u sobu 4 x 3 i bez okretanja, a 2,9 x 3,5 ne staje nikako; 3,5 x 2,5 staje bez okretanja.
        // Predmet 2,5 x 3,9 ne staje kako jeste (3,9 > 3), ali staje okrenut (3,9 <= 4 i 2,5 <= 3).
        val v = validOf(input(length = "2,5", width = "3,9"))
        assertEquals(3.9, v.widthM, 1e-9)
    }

    @Test
    fun tooBigIsNotReportedWhileASizeIsStillInvalid() {
        val e = errorsOf(input(length = "", width = "9"))
        assertFalse(e.tooBigForRoom)
    }

    @Test
    fun allProblemsAreReportedAtOnce() {
        val e = errorsOf(FurnitureInput("", null, "", "abc", "9"))
        assertEquals(FurnitureNameError.EMPTY, e.name)
        assertTrue(e.categoryMissing)
        assertEquals(DimensionError.MISSING, e.length)
        assertEquals(DimensionError.NOT_A_NUMBER, e.width)
        assertEquals(DimensionError.TOO_LARGE, e.height)
    }

    @Test
    fun footprintSwapsLengthAndWidthOnOddTurns() {
        val f = Furniture(
            "f", "r", "Krevet", FurnitureCategory.BED, 2.0, 1.6, 0.5, 0.0, 0.0, 0,
            DimensionSource.USER_CONFIRMED, 1L
        )
        assertEquals(2.0, f.footprintLengthM, 0.0)
        assertEquals(1.6, f.footprintWidthM, 0.0)
        assertEquals(1.6, f.copy(quarterTurns = 1).footprintLengthM, 0.0)
        assertEquals(2.0, f.copy(quarterTurns = 1).footprintWidthM, 0.0)
        assertEquals(2.0, f.copy(quarterTurns = 2).footprintLengthM, 0.0)
        assertEquals(1.6, f.copy(quarterTurns = 3).footprintLengthM, 0.0)
    }
}
