package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ImageRescaleTest {

    // Slika 800 x 600 piksela, 1 cm po pikselu (duža strana 8 m), gornji levi ugao na (0, 0)
    private val image = PlanImage(
        projectId = "p", fileName = "f.img", widthPx = 800, heightPx = 600,
        metersPerPixel = 0.01, originXM = 0.0, originYM = 0.0,
        dimensionSource = DimensionSource.IMPORTED_CALIBRATED, createdAtMillis = 1L
    )

    @Test
    fun factorIsTheTypedMeasureOverTheDrawnOne() {
        assertEquals(1.25, ImageRescale.factor(4.0, 5.0)!!, 1e-9)
        assertEquals(0.75, ImageRescale.factor(4.0, 3.0)!!, 1e-9)
    }

    @Test
    fun factorsOutsideTheAllowedRangeAreRefused() {
        assertNull(ImageRescale.factor(4.0, 1.9)) // 0,475
        assertNull(ImageRescale.factor(4.0, 8.1)) // 2,025
        assertNotNull(ImageRescale.factor(4.0, 2.0)) // tačno 0,5
        assertNotNull(ImageRescale.factor(4.0, 8.0)) // tačno 2
    }

    @Test
    fun measuresThatAreNotPositiveGiveNoFactor() {
        assertNull(ImageRescale.factor(0.0, 4.0))
        assertNull(ImageRescale.factor(4.0, 0.0))
        assertNull(ImageRescale.factor(-1.0, 4.0))
        assertNull(ImageRescale.factor(Double.NaN, 4.0))
    }

    @Test
    fun theScaleIsMultipliedByTheFactor() {
        val scaled = ImageRescale.apply(image, 2.0, 2.0, 3.0)!!
        assertEquals(0.02, scaled.metersPerPixel, 1e-12)
        assertEquals(800, scaled.widthPx)
        assertEquals(600, scaled.heightPx)
        assertEquals("f.img", scaled.fileName)
        assertEquals(DimensionSource.IMPORTED_CALIBRATED, scaled.dimensionSource)
    }

    @Test
    fun thePointTheImageIsScaledAroundStaysInPlace() {
        // Piksel (200, 300) je na planu u (2, 3) pre i posle skaliranja oko te tačke
        val k = 1.6
        val scaled = ImageRescale.apply(image, k, 2.0, 3.0)!!
        assertEquals(2.0, scaled.originXM + 200 * scaled.metersPerPixel, 1e-9)
        assertEquals(3.0, scaled.originYM + 300 * scaled.metersPerPixel, 1e-9)
    }

    @Test
    fun theOriginMovesAwayFromTheAnchorWhenEnlarging() {
        val scaled = ImageRescale.apply(image, 2.0, 2.0, 3.0)!!
        assertEquals(-2.0, scaled.originXM, 1e-9) // 2 + (0 - 2) * 2
        assertEquals(-3.0, scaled.originYM, 1e-9) // 3 + (0 - 3) * 2
    }

    @Test
    fun scalingAroundTheOriginKeepsTheOrigin() {
        val scaled = ImageRescale.apply(image, 1.5, 0.0, 0.0)!!
        assertEquals(0.0, scaled.originXM, 1e-9)
        assertEquals(0.0, scaled.originYM, 1e-9)
    }

    @Test
    fun aResultWithAnUnrealisticSizeIsRefused() {
        // Duža strana bi bila 0,8 m, a dozvoljeno je najmanje 1 m
        assertNull(ImageRescale.apply(image, 0.1, 0.0, 0.0))
        // Duža strana bi bila 800 m, a dozvoljeno je najviše 500 m
        assertNull(ImageRescale.apply(image, 100.0, 0.0, 0.0))
    }

    @Test
    fun aFactorThatIsNotPositiveIsRefused() {
        assertNull(ImageRescale.apply(image, 0.0, 0.0, 0.0))
        assertNull(ImageRescale.apply(image, -1.0, 0.0, 0.0))
    }

    @Test
    fun theDrawnRectKeepsItsCornerAndScalesItsSides() {
        val rect = ImageRescale.scaleRect(DrawnRect(1.0, 2.0, 4.0, 3.0), 1.5)
        assertEquals(DrawnRect(1.0, 2.0, 6.0, 4.5), rect)
    }
}
