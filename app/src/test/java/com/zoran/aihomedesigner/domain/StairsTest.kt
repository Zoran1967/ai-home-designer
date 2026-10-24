package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class StairsTest {

    private fun stairs(direction: Int = 0, x: Double = 1.0, y: Double = 2.0) = Stairs(
        id = "s1", projectId = "p", lowerLevel = 0, xM = x, yM = y,
        lengthM = 4.0, widthM = 1.0, directionDeg = direction,
        dimensionSource = DimensionSource.USER_CONFIRMED, createdAtMillis = 1L
    )

    private fun input(
        level: Int = 0, length: String = "4,2", width: String = "1", direction: Int = 0
    ) = StairsInput(level, length, width, direction)

    // --- model ---

    @Test
    fun upperLevelIsOneAboveTheLowerLevel() {
        assertEquals(1, stairs().upperLevel)
        assertEquals(0, stairs().copy(lowerLevel = -1).upperLevel)
    }

    @Test
    fun footprintSwapsLengthAndWidthWhenGoingNorthOrSouth() {
        assertEquals(StairsFootprint(1.0, 2.0, 5.0, 3.0), StairsGeometry.footprint(stairs(0)))
        assertEquals(StairsFootprint(1.0, 2.0, 5.0, 3.0), StairsGeometry.footprint(stairs(180)))
        assertEquals(StairsFootprint(1.0, 2.0, 2.0, 6.0), StairsGeometry.footprint(stairs(90)))
        assertEquals(StairsFootprint(1.0, 2.0, 2.0, 6.0), StairsGeometry.footprint(stairs(270)))
    }

    @Test
    fun newIdsAreUnique() {
        assertNotEquals(Stairs.newId(), Stairs.newId())
    }

    // --- validator ---

    @Test
    fun validInputGivesMeters() {
        val result = StairsValidator.validate(input(length = "4,2", width = "0.9")) as StairsValidation.Valid
        assertEquals(4.2, result.lengthM, 1e-9)
        assertEquals(0.9, result.widthM, 1e-9)
        assertEquals(0, result.lowerLevel)
        assertEquals(0, result.directionDeg)
    }

    @Test
    fun directionIsNormalizedToZeroThrough270() {
        val a = StairsValidator.validate(input(direction = 450)) as StairsValidation.Valid
        assertEquals(90, a.directionDeg)
        val b = StairsValidator.validate(input(direction = -90)) as StairsValidation.Valid
        assertEquals(270, b.directionDeg)
    }

    @Test
    fun directionThatIsNotAMultipleOf90IsRejected() {
        val result = StairsValidator.validate(input(direction = 45)) as StairsValidation.Invalid
        assertTrue(result.errors.directionInvalid)
    }

    @Test
    fun floorsOutsideTheLimitsAreRejectedOnBothEnds() {
        val top = StairsValidator.validate(input(level = Floors.MAX_LEVEL)) as StairsValidation.Invalid
        assertTrue(top.errors.levelInvalid) // sprat iznad ne postoji
        val bottom = StairsValidator.validate(input(level = Floors.MIN_LEVEL - 1)) as StairsValidation.Invalid
        assertTrue(bottom.errors.levelInvalid)
        assertTrue(StairsValidator.validate(input(level = Floors.MIN_LEVEL)) is StairsValidation.Valid)
        assertTrue(StairsValidator.validate(input(level = Floors.MAX_LEVEL - 1)) is StairsValidation.Valid)
    }

    @Test
    fun measureErrorsAreReportedSeparately() {
        val result = StairsValidator.validate(input(length = "", width = "abc")) as StairsValidation.Invalid
        assertEquals(DimensionError.MISSING, result.errors.length)
        assertEquals(DimensionError.NOT_A_NUMBER, result.errors.width)
        assertTrue(!result.errors.levelInvalid)
    }

    @Test
    fun measuresOutsideTheLimitsAreRejected() {
        val small = StairsValidator.validate(input(length = "0,5", width = "0,3")) as StairsValidation.Invalid
        assertEquals(DimensionError.TOO_SMALL, small.errors.length)
        assertEquals(DimensionError.TOO_SMALL, small.errors.width)
        val large = StairsValidator.validate(input(length = "9", width = "3,5")) as StairsValidation.Invalid
        assertEquals(DimensionError.TOO_LARGE, large.errors.length)
        assertEquals(DimensionError.TOO_LARGE, large.errors.width)
    }

    @Test
    fun measuresExactlyOnTheLimitsAreAccepted() {
        assertTrue(StairsValidator.validate(input(length = "1", width = "0,5")) is StairsValidation.Valid)
        assertTrue(StairsValidator.validate(input(length = "8", width = "3")) is StairsValidation.Valid)
    }

    // --- udobnost ---

    @Test
    fun stepCountFollowsTheTargetRiser() {
        assertEquals(15, StairsGeometry.stepCount(2.7))
        assertEquals(16, StairsGeometry.stepCount(2.85))
    }

    @Test
    fun stepCountIsAtLeastTwo() {
        assertEquals(2, StairsGeometry.stepCount(0.1))
    }

    @Test
    fun stepCountRejectsNonPositiveRise() {
        assertThrows(IllegalArgumentException::class.java) { StairsGeometry.stepCount(0.0) }
        assertThrows(IllegalArgumentException::class.java) { StairsGeometry.stepCount(-1.0) }
        assertThrows(IllegalArgumentException::class.java) { StairsGeometry.stepCount(Double.NaN) }
    }

    @Test
    fun comfortableStairsGetNoAdvice() {
        // 2,85 m uspona, 16 stepenika, 15 gazišta po 0,28 m = 4,2 m kraka
        val comfort = StairsGeometry.analyze(2.85, 4.2)
        assertEquals(16, comfort.stepCount)
        assertEquals(2.85 / 16, comfort.riserM, 1e-9)
        assertEquals(0.28, comfort.treadM, 1e-9)
        assertTrue(comfort.advice.isEmpty())
    }

    @Test
    fun aShortRunGivesAShortTreadAdvice() {
        val comfort = StairsGeometry.analyze(2.85, 3.0) // 3,0 / 15 = 0,2 m
        assertEquals(listOf(StairsAdvice.TREAD_TOO_SHORT), comfort.advice)
    }

    @Test
    fun aHighRiserIsFlaggedWhenTheRiseNeedsOnlyTwoSteps() {
        // 0,4 m uspona: 0,4 / 0,18 = 2,2 -> 2 stepenika od po 0,2 m, što je više od preporučenih 0,19 m
        val comfort = StairsGeometry.analyze(0.4, 2.0)
        assertEquals(2, comfort.stepCount)
        assertEquals(0.2, comfort.riserM, 1e-9)
        assertEquals(listOf(StairsAdvice.RISER_TOO_HIGH), comfort.advice)
    }

    @Test
    fun bothAdvicesCanAppearTogether() {
        val comfort = StairsGeometry.analyze(0.4, 0.2) // stepenik 0,2 m i gazište 0,2 m
        assertEquals(listOf(StairsAdvice.RISER_TOO_HIGH, StairsAdvice.TREAD_TOO_SHORT), comfort.advice)
    }

    @Test
    fun analyzeRejectsNonPositiveRun() {
        assertThrows(IllegalArgumentException::class.java) { StairsGeometry.analyze(2.85, 0.0) }
    }
}
