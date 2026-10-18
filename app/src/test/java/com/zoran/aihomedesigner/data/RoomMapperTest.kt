package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Room
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomMapperTest {

    @Test
    fun roomWithThicknessSurvivesRoundTrip() {
        val room = Room(
            "r1", "p1", "Kuhinja", 3.2, 2.4, 2.6, 0.25,
            DimensionSource.USER_CONFIRMED, 1_700_000_000_000L
        )
        assertEquals(room, room.toEntity().toDomainOrNull())
    }

    @Test
    fun roomWithoutThicknessKeepsNull() {
        val room = Room(
            "r2", "p1", "Hodnik", 4.0, 1.2, 2.5, null,
            DimensionSource.USER_CONFIRMED, 2L
        )
        val back = room.toEntity().toDomainOrNull()
        assertNull(back!!.wallThicknessM)
    }

    @Test
    fun positionSurvivesRoundTrip() {
        val room = Room(
            "r5", "p1", "Soba", 3.0, 3.0, 2.6, null,
            DimensionSource.USER_CONFIRMED, 5L,
            originXM = 5.25, originYM = 1.5
        )
        val back = room.toEntity().toDomainOrNull()!!
        assertEquals(5.25, back.originXM, 0.0)
        assertEquals(1.5, back.originYM, 0.0)
    }

    @Test
    fun dimensionSourceIsStoredByName() {
        val entity = Room(
            "r3", "p1", "Soba", 3.0, 3.0, 2.6, null,
            DimensionSource.ESTIMATED, 3L
        ).toEntity()
        assertEquals("ESTIMATED", entity.dimensionSource)
    }

    @Test
    fun unknownSourceGivesNullInsteadOfWrongData() {
        val entity = RoomEntity("r4", "p1", "Soba", 3.0, 3.0, 2.6, null, "GUESS", 4L)
        assertNull(entity.toDomainOrNull())
    }
}
