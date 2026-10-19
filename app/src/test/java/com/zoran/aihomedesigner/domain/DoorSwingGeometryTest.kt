package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DoorSwingGeometryTest {

    // Soba 4 m (x) x 3 m (y), gornji levi ugao u (0, 0)
    private val room = Room(
        id = "r",
        projectId = "p",
        name = "Soba",
        lengthM = 4.0,
        widthM = 3.0,
        heightM = 2.6,
        wallThicknessM = null,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L,
        xM = 0.0,
        yM = 0.0
    )

    private fun door(
        side: WallSide,
        swing: DoorSwing? = DoorSwing.INTO_ROOM,
        hinge: HingeEnd? = HingeEnd.START,
        offset: Double = 1.0,
        width: Double = 0.9,
        type: OpeningType = OpeningType.DOOR
    ) = Opening(
        id = "o",
        roomId = "r",
        type = type,
        side = side,
        offsetM = offset,
        widthM = width,
        heightM = 2.05,
        sillHeightM = 0.0,
        swing = swing,
        hinge = hinge,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L
    )

    private fun assertPoint(expectedX: Double, expectedY: Double, actual: PlanPoint) {
        assertEquals(expectedX, actual.xM, 1e-9)
        assertEquals(expectedY, actual.yM, 1e-9)
    }

    @Test
    fun northDoorOpeningIntoRoomSwingsDownFromTheHinge() {
        val s = DoorSwingGeometry.shape(door(WallSide.NORTH), room)!!
        assertPoint(1.0, 0.0, s.hinge)
        assertPoint(1.9, 0.0, s.closedEnd)
        assertPoint(1.0, 0.9, s.openEnd)
        assertEquals(0.9, s.radiusM, 1e-9)
    }

    @Test
    fun northDoorOpeningOutOfRoomSwingsUpOutsideTheRoom() {
        val s = DoorSwingGeometry.shape(door(WallSide.NORTH, swing = DoorSwing.OUT_OF_ROOM), room)!!
        assertPoint(1.0, 0.0, s.hinge)
        assertPoint(1.0, -0.9, s.openEnd)
    }

    @Test
    fun hingeAtTheEndOfTheOpeningMovesTheHingeToTheOtherSide() {
        val s = DoorSwingGeometry.shape(door(WallSide.NORTH, hinge = HingeEnd.END), room)!!
        assertPoint(1.9, 0.0, s.hinge)
        assertPoint(1.0, 0.0, s.closedEnd)
        assertPoint(1.9, 0.9, s.openEnd)
    }

    @Test
    fun southDoorOpeningIntoRoomSwingsUp() {
        val s = DoorSwingGeometry.shape(door(WallSide.SOUTH), room)!!
        assertPoint(1.0, 3.0, s.hinge)
        assertPoint(1.9, 3.0, s.closedEnd)
        assertPoint(1.0, 2.1, s.openEnd)
    }

    @Test
    fun eastDoorOpeningIntoRoomSwingsLeft() {
        val s = DoorSwingGeometry.shape(door(WallSide.EAST), room)!!
        assertPoint(4.0, 1.0, s.hinge)
        assertPoint(4.0, 1.9, s.closedEnd)
        assertPoint(3.1, 1.0, s.openEnd)
    }

    @Test
    fun westDoorOpeningIntoRoomSwingsRight() {
        val s = DoorSwingGeometry.shape(door(WallSide.WEST), room)!!
        assertPoint(0.0, 1.0, s.hinge)
        assertPoint(0.0, 1.9, s.closedEnd)
        assertPoint(0.9, 1.0, s.openEnd)
    }

    @Test
    fun shapeFollowsTheRoomPositionOnThePlan() {
        val moved = room.copy(xM = 10.0, yM = 5.0)
        val s = DoorSwingGeometry.shape(door(WallSide.NORTH), moved)!!
        assertPoint(11.0, 5.0, s.hinge)
        assertPoint(11.0, 5.9, s.openEnd)
    }

    @Test
    fun slidingDoorHasNoSwing() {
        assertNull(DoorSwingGeometry.shape(door(WallSide.NORTH, swing = DoorSwing.SLIDING, hinge = null), room))
    }

    @Test
    fun windowHasNoSwing() {
        val window = door(WallSide.NORTH, swing = null, hinge = null, type = OpeningType.WINDOW)
        assertNull(DoorSwingGeometry.shape(window, room))
    }

    @Test
    fun doorWithoutHingeHasNoShape() {
        assertNull(DoorSwingGeometry.shape(door(WallSide.NORTH, hinge = null), room))
    }

    @Test
    fun roomWithoutPositionHasNoShape() {
        assertNull(DoorSwingGeometry.shape(door(WallSide.NORTH), room.copy(xM = null, yM = null)))
    }

    @Test
    fun openEndIsAlwaysOneDoorWidthFromTheHingeAtARightAngleToTheWall() {
        for (side in WallSide.values()) {
            for (hinge in HingeEnd.values()) {
                for (swing in listOf(DoorSwing.INTO_ROOM, DoorSwing.OUT_OF_ROOM)) {
                    val s = DoorSwingGeometry.shape(door(side, swing = swing, hinge = hinge), room)
                    assertNotNull(s)
                    s!!
                    val wallX = s.closedEnd.xM - s.hinge.xM
                    val wallY = s.closedEnd.yM - s.hinge.yM
                    val openX = s.openEnd.xM - s.hinge.xM
                    val openY = s.openEnd.yM - s.hinge.yM
                    assertEquals(0.0, wallX * openX + wallY * openY, 1e-9)
                    assertEquals(0.9, Math.hypot(openX, openY), 1e-9)
                }
            }
        }
    }
}
