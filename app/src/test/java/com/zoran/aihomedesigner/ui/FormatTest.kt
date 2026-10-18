package com.zoran.aihomedesigner.ui

import com.zoran.aihomedesigner.domain.RoomInput
import com.zoran.aihomedesigner.domain.RoomValidation
import com.zoran.aihomedesigner.domain.RoomValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {

    @Test
    fun inputTextHasNoTrailingZerosAndUsesComma() {
        assertEquals("4,5", metersToInputText(4.5))
        assertEquals("4", metersToInputText(4.0))
        assertEquals("50", metersToInputText(50.0))
        assertEquals("3,4567", metersToInputText(3.4567))
        assertEquals("0,05", metersToInputText(0.05))
    }

    @Test
    fun editTextGivesBackExactlyTheSameValueWhenSavedAgain() {
        val values = listOf(0.5, 0.05, 1.8, 2.6, 3.2, 3.456, 3.4567, 4.5, 6.0, 12.34, 50.0)
        for (value in values) {
            val text = metersToInputText(value)
            val result = RoomValidator.validate(
                RoomInput("Soba", text, text, "2,6", ""),
                emptyList()
            )
            if (value in 0.5..50.0) {
                assertTrue("Vrednost $value ($text) nije prošla proveru", result is RoomValidation.Valid)
                result as RoomValidation.Valid
                assertEquals(value, result.lengthM, 0.0)
                assertEquals(value, result.widthM, 0.0)
            }
        }
    }
}
