package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.DoorSwing
import com.zoran.aihomedesigner.domain.HingeEnd
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.OpeningType
import com.zoran.aihomedesigner.domain.WallSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningMapperTest {

    private val door = Opening(
        id = "o1",
        roomId = "r1",
        type = OpeningType.DOOR,
        side = WallSide.SOUTH,
        offsetM = 1.25,
        widthM = 0.9,
        heightM = 2.05,
        sillHeightM = 0.0,
        swing = DoorSwing.OUT_OF_ROOM,
        hinge = HingeEnd.END,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1_700_000_000_000L
    )

    private val window = Opening(
        id = "o2",
        roomId = "r1",
        type = OpeningType.WINDOW,
        side = WallSide.EAST,
        offsetM = 0.5,
        widthM = 1.2,
        heightM = 1.2,
        sillHeightM = 0.9,
        swing = null,
        hinge = null,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 2L
    )

    @Test
    fun doorSurvivesRoundTrip() {
        assertEquals(door, door.toEntity().toDomainOrNull())
    }

    @Test
    fun windowKeepsNullSwingAndHinge() {
        val back = window.toEntity().toDomainOrNull()!!
        assertEquals(window, back)
        assertNull(back.swing)
        assertNull(back.hinge)
    }

    @Test
    fun enumsAreStoredByName() {
        val entity = door.toEntity()
        assertEquals("DOOR", entity.type)
        assertEquals("SOUTH", entity.side)
        assertEquals("OUT_OF_ROOM", entity.swing)
        assertEquals("END", entity.hinge)
        assertEquals("USER_CONFIRMED", entity.dimensionSource)
    }

    @Test
    fun unknownValuesGiveNullInsteadOfWrongData() {
        val entity = door.toEntity()
        assertNull(entity.copy(type = "GATE").toDomainOrNull())
        assertNull(entity.copy(side = "UP").toDomainOrNull())
        assertNull(entity.copy(swing = "SPIN").toDomainOrNull())
        assertNull(entity.copy(hinge = "MIDDLE").toDomainOrNull())
        assertNull(entity.copy(dimensionSource = "GUESS").toDomainOrNull())
    }

    @Test
    fun newIdsAreUnique() {
        assertNotEquals(Opening.newId(), Opening.newId())
    }
}
