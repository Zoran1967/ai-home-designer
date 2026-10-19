package com.zoran.aihomedesigner.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatCentimetersTest {

    @Test
    fun wholeCentimetresAreShownWithoutDecimals() {
        assertEquals("20", formatCentimeters(0.2))
        assertEquals("25", formatCentimeters(0.25))
        assertEquals("5", formatCentimeters(0.05))
        assertEquals("0", formatCentimeters(0.0))
    }

    @Test
    fun floatingPointNoiseIsNotShown() {
        assertEquals("20", formatCentimeters(4.2 - 4.0))
    }
}
