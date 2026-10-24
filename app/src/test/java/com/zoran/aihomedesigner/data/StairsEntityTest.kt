package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Stairs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StairsEntityTest {

    private val stairs = Stairs(
        id = "s1",
        projectId = "p1",
        lowerLevel = -1,
        xM = 1.25,
        yM = -0.5,
        lengthM = 4.2,
        widthM = 0.9,
        directionDeg = 270,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1_700_000_000_000L
    )

    @Test
    fun stairsSurviveRoundTrip() {
        assertEquals(stairs, stairs.toEntity().toDomainOrNull())
    }

    @Test
    fun enumIsStoredByNameAndDirectionAsANumber() {
        val entity = stairs.toEntity()
        assertEquals("USER_CONFIRMED", entity.dimensionSource)
        assertEquals(270, entity.directionDeg)
        assertEquals(-1, entity.lowerLevel)
    }

    @Test
    fun unknownSourceInTheDatabaseGivesNull() {
        assertNull(stairs.toEntity().copy(dimensionSource = "NEPOZNATO").toDomainOrNull())
    }

    @Test
    fun directionThatIsNotARightAngleGivesNull() {
        assertNull(stairs.toEntity().copy(directionDeg = 45).toDomainOrNull())
        assertNull(stairs.toEntity().copy(directionDeg = 360).toDomainOrNull())
        assertNotNull(stairs.toEntity().copy(directionDeg = 0).toDomainOrNull())
    }
}
