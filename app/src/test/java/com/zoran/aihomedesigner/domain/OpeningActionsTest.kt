package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningActionsTest {

    private fun room(id: String = "r", x: Double? = 1.0, y: Double? = 2.0) = Room(
        id = id, projectId = "p", name = id, lengthM = 4.0, widthM = 3.0, heightM = 2.6,
        wallThicknessM = null, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = x, yM = y, floorLevel = 0
    )

    private fun opening(
        id: String = "o", roomId: String = "r", type: OpeningType = OpeningType.DOOR,
        side: WallSide = WallSide.NORTH, offset: Double = 1.0, width: Double = 0.9,
        swing: DoorSwing? = DoorSwing.INTO_ROOM, hinge: HingeEnd? = HingeEnd.START
    ) = Opening(
        id = id, roomId = roomId, type = type, side = side, offsetM = offset, widthM = width,
        heightM = if (type == OpeningType.DOOR) 2.05 else 1.2,
        sillHeightM = if (type == OpeningType.DOOR) 0.0 else 0.9,
        swing = if (type == OpeningType.DOOR) swing else null,
        hinge = if (type == OpeningType.DOOR) hinge else null,
        dimensionSource = DimensionSource.USER_CONFIRMED, createdAtMillis = 1L
    )

    // --- okretanje ---

    @Test
    fun flippingTheSwingKeepsEverythingElse() {
        val input = OpeningActions.flipSwing(opening(swing = DoorSwing.INTO_ROOM, hinge = HingeEnd.END))!!
        assertEquals(DoorSwing.OUT_OF_ROOM, input.swing)
        assertEquals(HingeEnd.END, input.hinge)
        assertEquals("1", input.offset)
        assertEquals("0,9", input.width)
        assertEquals("2,05", input.height)
        assertEquals("", input.sillHeight)
        assertEquals(WallSide.NORTH, input.side)
    }

    @Test
    fun flippingTheSwingTwiceGoesBack() {
        assertEquals(
            DoorSwing.INTO_ROOM,
            OpeningActions.flipSwing(opening(swing = DoorSwing.OUT_OF_ROOM))!!.swing
        )
    }

    @Test
    fun flippingTheHingeKeepsTheSwing() {
        val input = OpeningActions.flipHinge(opening(swing = DoorSwing.OUT_OF_ROOM, hinge = HingeEnd.START))!!
        assertEquals(HingeEnd.END, input.hinge)
        assertEquals(DoorSwing.OUT_OF_ROOM, input.swing)
        assertEquals(HingeEnd.START, OpeningActions.flipHinge(opening(hinge = HingeEnd.END))!!.hinge)
    }

    @Test
    fun slidingDoorsAndWindowsCannotBeFlipped() {
        val sliding = opening(swing = DoorSwing.SLIDING, hinge = null)
        assertTrue(!OpeningActions.canFlip(sliding))
        assertNull(OpeningActions.flipSwing(sliding))
        assertNull(OpeningActions.flipHinge(sliding))
        val window = opening(type = OpeningType.WINDOW)
        assertTrue(!OpeningActions.canFlip(window))
        assertNull(OpeningActions.flipSwing(window))
        assertNull(OpeningActions.flipHinge(window))
    }

    @Test
    fun aFlippedDoorPassesTheNormalValidationWithTheSameMeasures() {
        val door = opening(offset = 1.25, width = 0.85)
        val input = OpeningActions.flipSwing(door)!!
        val valid = OpeningValidator.validate(input, room(), emptyList(), editingId = door.id)
            as OpeningValidation.Valid
        assertEquals(1.25, valid.offsetM, 0.0)
        assertEquals(0.85, valid.widthM, 0.0)
        assertEquals(2.05, valid.heightM, 0.0)
        assertEquals(0.0, valid.sillHeightM, 0.0)
        assertEquals(DoorSwing.OUT_OF_ROOM, valid.swing)
    }

    @Test
    fun inputForAWindowKeepsTheSillHeight() {
        val input = OpeningActions.input(opening(type = OpeningType.WINDOW), null, null)
        assertEquals("0,9", input.sillHeight)
        assertEquals("1,2", input.height)
    }

    // --- pronalaženje dodirnutog otvora ---

    @Test
    fun tapOnTheDoorLineIsAHit() {
        // soba počinje u (1, 2), vrata na severnom zidu: x od 2,0 do 2,9 na y = 2
        val hit = OpeningActions.nearestOpening(listOf(room()), listOf(opening()), 2.5, 2.1, 0.2)
        assertNotNull(hit)
    }

    @Test
    fun tapBesideTheDoorOnTheSameWallIsNotAHit() {
        assertNull(OpeningActions.nearestOpening(listOf(room()), listOf(opening()), 3.5, 2.0, 0.2))
        assertNotNull(OpeningActions.nearestOpening(listOf(room()), listOf(opening()), 2.95, 2.0, 0.1))
    }

    @Test
    fun allFourWallsAreSupported() {
        val r = room()
        val openings = listOf(
            opening("n", side = WallSide.NORTH, offset = 1.0),
            opening("s", side = WallSide.SOUTH, offset = 1.0),
            opening("w", side = WallSide.WEST, offset = 0.5, width = 1.0),
            opening("e", side = WallSide.EAST, offset = 0.5, width = 1.0)
        )
        assertEquals("n", OpeningActions.nearestOpening(listOf(r), openings, 2.5, 2.0, 0.1)!!.id)
        assertEquals("s", OpeningActions.nearestOpening(listOf(r), openings, 2.5, 5.0, 0.1)!!.id)
        assertEquals("w", OpeningActions.nearestOpening(listOf(r), openings, 1.0, 3.0, 0.1)!!.id)
        assertEquals("e", OpeningActions.nearestOpening(listOf(r), openings, 5.0, 3.0, 0.1)!!.id)
    }

    @Test
    fun theNearestOfTwoOpeningsWins() {
        val r = room()
        val a = opening("a", offset = 0.2, width = 0.8)  // x od 1,2 do 2,0
        val b = opening("b", offset = 1.5, width = 0.8)  // x od 2,5 do 3,3
        assertEquals("b", OpeningActions.nearestOpening(listOf(r), listOf(a, b), 2.4, 2.0, 0.5)!!.id)
        assertEquals("a", OpeningActions.nearestOpening(listOf(r), listOf(a, b), 2.1, 2.0, 0.5)!!.id)
    }

    @Test
    fun openingsOfUnplacedOrMissingRoomsAreSkipped() {
        val unplaced = room("r", x = null, y = null)
        assertNull(OpeningActions.nearestOpening(listOf(unplaced), listOf(opening()), 0.0, 0.0, 10.0))
        assertNull(OpeningActions.nearestOpening(emptyList(), listOf(opening()), 0.0, 0.0, 10.0))
    }
}
