package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.DoorSwing
import com.zoran.aihomedesigner.domain.HingeSide
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.OpeningType
import com.zoran.aihomedesigner.domain.WallSide
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpeningMapperTest {

    @Test
    fun doorSurvivesRoundTrip() {
        val door = Opening(
            "d1", "r1", WallSide.WEST, OpeningType.DOOR, 0.5, 0.9, 2.05, 0.0,
            DoorSwing.OUT_OF_ROOM, HingeSide.END, DimensionSource.USER_CONFIRMED, 1L
        )
        assertEquals(door, door.toEntity().toDomainOrNull())
    }

    @Test
    fun windowKeepsNullSwingAndHinge() {
        val window = Opening(
            "w1", "r1", WallSide.NORTH, OpeningType.WINDOW, 1.0, 1.2, 1.2, 0.9,
            null, null, DimensionSource.USER_CONFIRMED, 2L
        )
        val back = window.toEntity().toDomainOrNull()!!
        assertNull(back.swing)
        assertNull(back.hinge)
        assertEquals(window, back)
    }

    @Test
    fun enumsAreStoredByName() {
        val entity = Opening(
            "d2", "r1", WallSide.SOUTH, OpeningType.DOOR, 0.0, 0.8, 2.0, 0.0,
            DoorSwing.INTO_ROOM, HingeSide.START, DimensionSource.ESTIMATED, 3L
        ).toEntity()
        assertEquals("SOUTH", entity.side)
        assertEquals("DOOR", entity.type)
        assertEquals("INTO_ROOM", entity.swing)
        assertEquals("START", entity.hinge)
        assertEquals("ESTIMATED", entity.dimensionSource)
    }

    @Test
    fun unknownValuesGiveNullInsteadOfWrongData() {
        val base = OpeningEntity(
            "x", "r1", "NORTH", "DOOR", 0.0, 0.9, 2.0, 0.0, "INTO_ROOM", "START", "USER_CONFIRMED", 1L
        )
        assertNull(base.copy(side = "UP").toDomainOrNull())
        assertNull(base.copy(type = "GATE").toDomainOrNull())
        assertNull(base.copy(swing = "SIDEWAYS").toDomainOrNull())
        assertNull(base.copy(hinge = "MIDDLE").toDomainOrNull())
        assertNull(base.copy(dimensionSource = "GUESS").toDomainOrNull())
    }
}
