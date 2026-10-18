package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.Project
import com.zoran.aihomedesigner.domain.PropertyType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProjectMapperTest {

    @Test
    fun projectSurvivesRoundTripThroughEntity() {
        val project = Project(
            id = "abc-123",
            name = "Moj stan",
            type = PropertyType.APARTMENT,
            createdAtMillis = 1_700_000_000_000L
        )
        assertEquals(project, project.toEntity().toDomainOrNull())
    }

    @Test
    fun typeIsStoredByNameNotByNumber() {
        val entity = Project("id", "Kuća", PropertyType.HOUSE, 1L).toEntity()
        assertEquals("HOUSE", entity.type)
    }

    @Test
    fun unknownTypeGivesNullInsteadOfWrongData() {
        val entity = ProjectEntity("id", "Nešto", "CASTLE", 1L)
        assertNull(entity.toDomainOrNull())
    }
}
