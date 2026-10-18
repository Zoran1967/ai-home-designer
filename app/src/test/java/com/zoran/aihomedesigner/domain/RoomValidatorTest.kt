package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomValidatorTest {

    private fun input(
        name: String = "Dnevna soba",
        length: String = "4,5",
        width: String = "3,2",
        height: String = "2,6",
        thickness: String = "0,2"
    ) = RoomInput(name, length, width, height, thickness)

    private fun errorsOf(input: RoomInput, existing: List<String> = emptyList()): RoomInputErrors {
        val result = RoomValidator.validate(input, existing)
        assertTrue("Očekivana greška, a unos je prošao", result is RoomValidation.Invalid)
        return (result as RoomValidation.Invalid).errors
    }

    private fun validOf(input: RoomInput, existing: List<String> = emptyList()): RoomValidation.Valid {
        val result = RoomValidator.validate(input, existing)
        assertTrue("Unos je trebalo da prođe", result is RoomValidation.Valid)
        return result as RoomValidation.Valid
    }

    @Test
    fun validInputPassesAndKeepsValues() {
        val v = validOf(input())
        assertEquals("Dnevna soba", v.name)
        assertEquals(4.5, v.lengthM, 1e-9)
        assertEquals(3.2, v.widthM, 1e-9)
        assertEquals(2.6, v.heightM, 1e-9)
        assertEquals(0.2, v.wallThicknessM!!, 1e-9)
    }

    @Test
    fun commaAndDotBothWork() {
        assertEquals(3.5, validOf(input(length = "3,5")).lengthM, 1e-9)
        assertEquals(3.5, validOf(input(length = "3.5")).lengthM, 1e-9)
    }

    @Test
    fun wholeNumbersWork() {
        assertEquals(4.0, validOf(input(length = "4")).lengthM, 1e-9)
    }

    @Test
    fun emptyDimensionIsMissing() {
        assertEquals(DimensionError.MISSING, errorsOf(input(length = "")).length)
        assertEquals(DimensionError.MISSING, errorsOf(input(width = "  ")).width)
        assertEquals(DimensionError.MISSING, errorsOf(input(height = "")).height)
    }

    @Test
    fun textIsNotANumber() {
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(input(length = "abc")).length)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(input(length = "3,5m")).length)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(input(length = "NaN")).length)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(input(length = "Infinity")).length)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(input(length = "-3")).length)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(input(length = "1e3")).length)
    }

    @Test
    fun tooSmallAndTooLargeLengthAreRejected() {
        assertEquals(DimensionError.TOO_SMALL, errorsOf(input(length = "0,3")).length)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(input(length = "60")).length)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(input(length = "350")).length)
    }

    @Test
    fun boundaryValuesAreAccepted() {
        validOf(input(length = "0,5", width = "50"))
        validOf(input(height = "1,8"))
        validOf(input(height = "6"))
    }

    @Test
    fun wallHeightLimitsAreChecked() {
        assertEquals(DimensionError.TOO_SMALL, errorsOf(input(height = "1,5")).height)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(input(height = "7")).height)
    }

    @Test
    fun blankThicknessMeansNotEntered() {
        assertNull(validOf(input(thickness = "")).wallThicknessM)
        assertNull(validOf(input(thickness = "   ")).wallThicknessM)
    }

    @Test
    fun thicknessIsCheckedWhenEntered() {
        assertEquals(DimensionError.TOO_LARGE, errorsOf(input(thickness = "2")).wallThickness)
        assertEquals(DimensionError.TOO_SMALL, errorsOf(input(thickness = "0,01")).wallThickness)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(input(thickness = "x")).wallThickness)
    }

    @Test
    fun emptyNameIsRejected() {
        assertEquals(RoomNameError.EMPTY, errorsOf(input(name = "  ")).name)
    }

    @Test
    fun tooLongNameIsRejected() {
        val name = "a".repeat(RoomValidator.MAX_NAME_LENGTH + 1)
        assertEquals(RoomNameError.TOO_LONG, errorsOf(input(name = name)).name)
        validOf(input(name = "a".repeat(RoomValidator.MAX_NAME_LENGTH)))
    }

    @Test
    fun duplicateNameInSameProjectIsRejectedIgnoringCaseAndSpaces() {
        val errors = errorsOf(input(name = "  dnevna   SOBA "), listOf("Dnevna soba"))
        assertEquals(RoomNameError.DUPLICATE, errors.name)
    }

    @Test
    fun allErrorsAreReportedAtOnce() {
        val errors = errorsOf(RoomInput("", "", "abc", "9", "5"))
        assertEquals(RoomNameError.EMPTY, errors.name)
        assertEquals(DimensionError.MISSING, errors.length)
        assertEquals(DimensionError.NOT_A_NUMBER, errors.width)
        assertEquals(DimensionError.TOO_LARGE, errors.height)
        assertEquals(DimensionError.TOO_LARGE, errors.wallThickness)
    }

    @Test
    fun nameIsNormalizedWhenValid() {
        assertEquals("Spavaća soba", validOf(input(name = "  Spavaća    soba ")).name)
    }

    @Test
    fun areaIsLengthTimesWidth() {
        val room = Room("id", "p", "Soba", 4.0, 2.5, 2.6, null, DimensionSource.USER_CONFIRMED, 1L)
        assertEquals(10.0, room.areaM2, 1e-9)
    }
}
