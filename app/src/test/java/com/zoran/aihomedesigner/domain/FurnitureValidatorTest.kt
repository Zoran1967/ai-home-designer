package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FurnitureValidatorTest {

    private fun input(
        name: String = "Krevet",
        length: String = "2",
        width: String = "1,6",
        height: String = "0,5",
        rotation: Int = 0
    ) = FurnitureInput(name, FurnitureCategory.BED, length, width, height, rotation)

    private fun errorsOf(input: FurnitureInput): FurnitureInputErrors {
        val result = FurnitureValidator.validate(input)
        assertTrue("Očekivana greška, a unos je prošao", result is FurnitureValidation.Invalid)
        return (result as FurnitureValidation.Invalid).errors
    }

    private fun validOf(input: FurnitureInput): FurnitureValidation.Valid {
        val result = FurnitureValidator.validate(input)
        assertTrue("Unos je trebalo da prođe: $result", result is FurnitureValidation.Valid)
        return result as FurnitureValidation.Valid
    }

    @Test
    fun validInputKeepsValues() {
        val v = validOf(input())
        assertEquals("Krevet", v.name)
        assertEquals(2.0, v.lengthM, 1e-9)
        assertEquals(1.6, v.widthM, 1e-9)
        assertEquals(0.5, v.heightM, 1e-9)
        assertEquals(0, v.rotationDeg)
    }

    @Test
    fun nameIsNormalizedAndChecked() {
        assertEquals("Radni sto", validOf(input(name = "  Radni    sto ")).name)
        assertEquals(FurnitureNameError.EMPTY, errorsOf(input(name = "  ")).name)
        val tooLong = "a".repeat(FurnitureLimits.MAX_NAME_LENGTH + 1)
        assertEquals(FurnitureNameError.TOO_LONG, errorsOf(input(name = tooLong)).name)
    }

    @Test
    fun dimensionProblemsAreReportedPerField() {
        assertEquals(DimensionError.MISSING, errorsOf(input(length = "")).length)
        assertEquals(DimensionError.NOT_A_NUMBER, errorsOf(input(width = "abc")).width)
        assertEquals(DimensionError.TOO_SMALL, errorsOf(input(height = "0,01")).height)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(input(length = "7")).length)
        assertEquals(DimensionError.TOO_LARGE, errorsOf(input(height = "4")).height)
    }

    @Test
    fun allProblemsAreReportedAtOnce() {
        val e = errorsOf(FurnitureInput("", FurnitureCategory.OTHER, "", "x", "9", 45))
        assertEquals(FurnitureNameError.EMPTY, e.name)
        assertEquals(DimensionError.MISSING, e.length)
        assertEquals(DimensionError.NOT_A_NUMBER, e.width)
        assertEquals(DimensionError.TOO_LARGE, e.height)
        assertTrue(e.rotationInvalid)
    }

    @Test
    fun rotationIsNormalizedToRightAngles() {
        assertEquals(90, FurnitureValidator.normalizeRotation(90))
        assertEquals(90, FurnitureValidator.normalizeRotation(450))
        assertEquals(270, FurnitureValidator.normalizeRotation(-90))
        assertEquals(0, FurnitureValidator.normalizeRotation(360))
        assertNull(FurnitureValidator.normalizeRotation(45))
        assertEquals(270, validOf(input(rotation = -90)).rotationDeg)
    }

    @Test
    fun footprintSwapsLengthAndWidthWhenRotatedByARightAngle() {
        fun item(rotation: Int) = Furniture(
            "f", "r", "Krevet", FurnitureCategory.BED,
            lengthM = 2.0, widthM = 1.6, heightM = 0.5,
            xM = 0.0, yM = 0.0, rotationDeg = rotation,
            dimensionSource = DimensionSource.USER_CONFIRMED, createdAtMillis = 1L
        )
        assertEquals(2.0, item(0).footprintLengthM, 0.0)
        assertEquals(1.6, item(0).footprintWidthM, 0.0)
        assertEquals(1.6, item(90).footprintLengthM, 0.0)
        assertEquals(2.0, item(90).footprintWidthM, 0.0)
        assertEquals(2.0, item(180).footprintLengthM, 0.0)
        assertEquals(1.6, item(270).footprintLengthM, 0.0)
    }
}
