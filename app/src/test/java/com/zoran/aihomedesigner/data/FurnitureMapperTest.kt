package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FurnitureMapperTest {

    private val bed = Furniture(
        id = "f1",
        roomId = "r1",
        name = "Krevet",
        category = FurnitureCategory.BED,
        lengthM = 2.0,
        widthM = 1.6,
        heightM = 0.5,
        xM = 0.25,
        yM = 1.5,
        quarterTurns = 3,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1_700_000_000_000L
    )

    @Test
    fun furnitureSurvivesRoundTrip() {
        assertEquals(bed, bed.toEntity().toDomainOrNull())
    }

    @Test
    fun enumsAreStoredByName() {
        val entity = bed.toEntity()
        assertEquals("BED", entity.category)
        assertEquals("USER_CONFIRMED", entity.dimensionSource)
        assertEquals(3, entity.quarterTurns)
    }

    @Test
    fun unknownValuesGiveNullInsteadOfWrongData() {
        val entity = bed.toEntity()
        assertNull(entity.copy(category = "THRONE").toDomainOrNull())
        assertNull(entity.copy(dimensionSource = "GUESS").toDomainOrNull())
        assertNull(entity.copy(quarterTurns = 4).toDomainOrNull())
        assertNull(entity.copy(quarterTurns = -1).toDomainOrNull())
    }

    @Test
    fun newIdsAreUnique() {
        assertNotEquals(Furniture.newId(), Furniture.newId())
    }
}
