package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfPageChoiceTest {

    @Test
    fun validNumbersAreAccepted() {
        assertEquals(1, PdfPageChoice.parse("1", 5))
        assertEquals(3, PdfPageChoice.parse("3", 5))
        assertEquals(5, PdfPageChoice.parse("5", 5))
    }

    @Test
    fun spacesAroundTheNumberAreIgnored() {
        assertEquals(2, PdfPageChoice.parse("  2 ", 5))
    }

    @Test
    fun numbersOutsideTheRangeAreRejected() {
        assertNull(PdfPageChoice.parse("0", 5))
        assertNull(PdfPageChoice.parse("6", 5))
        assertNull(PdfPageChoice.parse("-1", 5))
    }

    @Test
    fun textThatIsNotAWholeNumberIsRejected() {
        assertNull(PdfPageChoice.parse("", 5))
        assertNull(PdfPageChoice.parse("   ", 5))
        assertNull(PdfPageChoice.parse("abc", 5))
        assertNull(PdfPageChoice.parse("2,5", 5))
        assertNull(PdfPageChoice.parse("2.5", 5))
        assertNull(PdfPageChoice.parse("1 2", 5))
    }

    @Test
    fun aDocumentWithoutPagesAcceptsNothing() {
        assertNull(PdfPageChoice.parse("1", 0))
    }
}
