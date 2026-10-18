package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectValidatorTest {

    @Test
    fun emptyNameIsRejected() {
        assertEquals(ProjectNameError.EMPTY, ProjectValidator.validateName("", emptyList()))
    }

    @Test
    fun blankNameIsRejected() {
        assertEquals(ProjectNameError.EMPTY, ProjectValidator.validateName("   ", emptyList()))
    }

    @Test
    fun nameOfMaxLengthIsAccepted() {
        val name = "a".repeat(ProjectValidator.MAX_NAME_LENGTH)
        assertNull(ProjectValidator.validateName(name, emptyList()))
    }

    @Test
    fun tooLongNameIsRejected() {
        val name = "a".repeat(ProjectValidator.MAX_NAME_LENGTH + 1)
        assertEquals(ProjectNameError.TOO_LONG, ProjectValidator.validateName(name, emptyList()))
    }

    @Test
    fun duplicateNameIsRejectedIgnoringCase() {
        val result = ProjectValidator.validateName("moj stan", listOf("Moj Stan"))
        assertEquals(ProjectNameError.DUPLICATE, result)
    }

    @Test
    fun duplicateNameIsRejectedIgnoringExtraSpaces() {
        val result = ProjectValidator.validateName("  Moj   stan ", listOf("Moj stan"))
        assertEquals(ProjectNameError.DUPLICATE, result)
    }

    @Test
    fun differentNameIsAccepted() {
        assertNull(ProjectValidator.validateName("Kuća na selu", listOf("Moj stan")))
    }

    @Test
    fun normalizeNameTrimsAndCollapsesSpaces() {
        assertEquals("Moj stan", ProjectValidator.normalizeName("  Moj    stan  "))
    }

    @Test
    fun newIdsAreUnique() {
        assertNotEquals(Project.newId(), Project.newId())
    }
}
