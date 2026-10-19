package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FurnitureEntityTest {

    private val bed = Furniture(
        id = "f1",
        roomId = "r1",
        name = "Bračni krevet",
        category = FurnitureCategory.BED,
        lengthM = 2.0,
        widthM = 1.6,
        heightM = 0.5,
        xM = 0.35,
        yM = 1.2,
        rotationDeg = 90,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1_700_000_000_000L
    )

    @Test
    fun furnitureSurvivesRoundTrip() {
        assertEquals(bed, bed.toEntity().toDomainOrNull())
    }

    @Test
    fun enumsAreStoredByNameAndRotationAsANumber() {
        val entity = bed.toEntity()
        assertEquals("BED", entity.category)
        assertEquals("USER_CONFIRMED", entity.dimensionSource)
        assertEquals(90, entity.rotationDeg)
    }

    @Test
    fun unknownValuesGiveNullInsteadOfWrongData() {
        val entity = bed.toEntity()
        assertNull(entity.copy(category = "SPACESHIP").toDomainOrNull())
        assertNull(entity.copy(dimensionSource = "GUESS").toDomainOrNull())
    }
}
