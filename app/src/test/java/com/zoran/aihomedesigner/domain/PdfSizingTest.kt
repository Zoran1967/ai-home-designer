package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfSizingTest {

    @Test
    fun portraitA4KeepsItsProportionsWithTheLongSideAtTheLimit() {
        val size = PdfSizing.sizeFor(595, 842)!!
        assertEquals(3000, size.heightPx)
        assertEquals(2120, size.widthPx)
    }

    @Test
    fun landscapePageHasTheLongSideAsTheWidth() {
        val size = PdfSizing.sizeFor(842, 595)!!
        assertEquals(3000, size.widthPx)
        assertEquals(2120, size.heightPx)
    }

    @Test
    fun squarePageStaysSquare() {
        val size = PdfSizing.sizeFor(500, 500, longSidePx = 1000)!!
        assertEquals(PixelSize(1000, 1000), size)
    }

    @Test
    fun invalidPageSizesGiveNull() {
        assertNull(PdfSizing.sizeFor(0, 842))
        assertNull(PdfSizing.sizeFor(595, -1))
        assertNull(PdfSizing.sizeFor(595, 842, longSidePx = 0))
    }

    @Test
    fun anExtremelyNarrowPageStillHasAtLeastOnePixel() {
        val size = PdfSizing.sizeFor(1, 100000)!!
        assertEquals(3000, size.heightPx)
        assertEquals(1, size.widthPx)
    }
}
