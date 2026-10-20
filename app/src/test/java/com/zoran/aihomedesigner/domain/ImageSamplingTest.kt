package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImageSamplingTest {

    @Test
    fun smallImageIsNotReduced() {
        assertEquals(1, ImageSampling.sampleSize(800, 600, 2048))
        assertEquals(1, ImageSampling.sampleSize(2048, 100, 2048))
    }

    @Test
    fun largeImageIsReducedByPowersOfTwoUntilItFits() {
        assertEquals(2, ImageSampling.sampleSize(4000, 3000, 2048))
        assertEquals(4, ImageSampling.sampleSize(8000, 6000, 2048))
        assertEquals(8, ImageSampling.sampleSize(12000, 9000, 2048))
    }

    @Test
    fun theLongerSideDecides() {
        assertEquals(2, ImageSampling.sampleSize(100, 4000, 2048))
    }

    @Test
    fun invalidMaximumIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { ImageSampling.sampleSize(100, 100, 0) }
    }
}
