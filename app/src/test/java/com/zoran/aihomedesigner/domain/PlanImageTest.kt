package com.zoran.aihomedesigner.domain

import com.zoran.aihomedesigner.data.toDomainOrNull
import com.zoran.aihomedesigner.data.toEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanImageTest {

    private fun ok(result: CalibrationResult): Double {
        assertTrue("Očekivana razmera, a rezultat je $result", result is CalibrationResult.Ok)
        return (result as CalibrationResult.Ok).metersPerPixel
    }

    private fun invalid(result: CalibrationResult): CalibrationResult.Invalid {
        assertTrue("Očekivana greška, a rezultat je $result", result is CalibrationResult.Invalid)
        return result as CalibrationResult.Invalid
    }

    @Test
    fun scaleIsKnownDistanceDividedByPixelDistance() {
        // tačke (100,100) i (400,500): 300 x 400 piksela = 500 piksela
        val mpp = ok(Calibration.calibrate(100.0, 100.0, 400.0, 500.0, "5", 1000, 800))
        assertEquals(0.01, mpp, 1e-12)
    }

    @Test
    fun commaAndDotInTheKnownDistanceBothWork() {
        val a = ok(Calibration.calibrate(0.0, 0.0, 350.0, 0.0, "3,5", 1000, 800))
        val b = ok(Calibration.calibrate(0.0, 0.0, 350.0, 0.0, "3.5", 1000, 800))
        assertEquals(0.01, a, 1e-12)
        assertEquals(a, b, 0.0)
    }

    @Test
    fun orderOfThePointsDoesNotMatter() {
        val a = ok(Calibration.calibrate(10.0, 20.0, 310.0, 420.0, "5", 1000, 800))
        val b = ok(Calibration.calibrate(310.0, 420.0, 10.0, 20.0, "5", 1000, 800))
        assertEquals(a, b, 0.0)
    }

    @Test
    fun pointsThatAreTooCloseAreRejected() {
        val e = invalid(Calibration.calibrate(100.0, 100.0, 105.0, 100.0, "5", 1000, 800))
        assertTrue(e.pointsTooClose)
        assertNull(e.distanceError)
    }

    @Test
    fun identicalPointsAreRejected() {
        assertTrue(invalid(Calibration.calibrate(50.0, 50.0, 50.0, 50.0, "5", 1000, 800)).pointsTooClose)
    }

    @Test
    fun badKnownDistanceIsReported() {
        assertEquals(
            DimensionError.MISSING,
            invalid(Calibration.calibrate(0.0, 0.0, 300.0, 0.0, "", 1000, 800)).distanceError
        )
        assertEquals(
            DimensionError.NOT_A_NUMBER,
            invalid(Calibration.calibrate(0.0, 0.0, 300.0, 0.0, "abc", 1000, 800)).distanceError
        )
        assertEquals(
            DimensionError.TOO_SMALL,
            invalid(Calibration.calibrate(0.0, 0.0, 300.0, 0.0, "0,05", 1000, 800)).distanceError
        )
        assertEquals(
            DimensionError.TOO_LARGE,
            invalid(Calibration.calibrate(0.0, 0.0, 300.0, 0.0, "150", 1000, 800)).distanceError
        )
    }

    @Test
    fun allProblemsAreReportedAtOnce() {
        val e = invalid(Calibration.calibrate(0.0, 0.0, 1.0, 0.0, "abc", 1000, 800))
        assertTrue(e.pointsTooClose)
        assertEquals(DimensionError.NOT_A_NUMBER, e.distanceError)
    }

    @Test
    fun anImplausiblyLargeImageMeansAProbableTypo() {
        // 100 m na 20 piksela, slika široka 1000 piksela = 5000 m
        val e = invalid(Calibration.calibrate(0.0, 0.0, 20.0, 0.0, "100", 1000, 800))
        assertTrue(e.implausibleScale)
    }

    @Test
    fun anImplausiblySmallImageMeansAProbableTypo() {
        // 0,1 m na 500 piksela, slika široka 1000 piksela = 0,2 m
        val e = invalid(Calibration.calibrate(0.0, 0.0, 500.0, 0.0, "0,1", 1000, 800))
        assertTrue(e.implausibleScale)
    }

    @Test
    fun imageSizeInMetersFollowsTheScale() {
        val image = PlanImage(
            "p", "plan_p.jpg", 1000, 800, 0.01, -1.0, 2.0,
            DimensionSource.IMPORTED_CALIBRATED, 1L
        )
        assertEquals(10.0, image.widthM, 1e-9)
        assertEquals(8.0, image.heightM, 1e-9)
    }

    @Test
    fun planImageSurvivesRoundTripThroughTheDatabaseEntity() {
        val image = PlanImage(
            "p", "plan_p.jpg", 1000, 800, 0.0125, -1.5, 2.25,
            DimensionSource.IMPORTED_CALIBRATED, 1_700_000_000_000L
        )
        assertEquals(image, image.toEntity().toDomainOrNull())
    }

    @Test
    fun unknownSourceInTheDatabaseGivesNull() {
        val image = PlanImage(
            "p", "plan_p.jpg", 1000, 800, 0.01, 0.0, 0.0,
            DimensionSource.IMPORTED_CALIBRATED, 1L
        )
        assertNull(image.toEntity().copy(dimensionSource = "GUESS").toDomainOrNull())
    }
}
