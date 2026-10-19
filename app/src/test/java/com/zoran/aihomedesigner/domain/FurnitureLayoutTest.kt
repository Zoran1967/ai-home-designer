package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FurnitureLayoutTest {

    // Soba 4 m (x) x 3 m (y), visine 2,6 m
    private val room = Room(
        id = "r", projectId = "p", name = "Soba",
        lengthM = 4.0, widthM = 3.0, heightM = 2.6,
        wallThicknessM = null, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = 0.0, yM = 0.0
    )

    private fun item(
        id: String = "f",
        x: Double,
        y: Double,
        length: Double = 2.0,
        width: Double = 1.0,
        height: Double = 0.8,
        rotation: Int = 0
    ) = Furniture(
        id, "r", "Predmet", FurnitureCategory.OTHER, length, width, height,
        x, y, rotation, DimensionSource.USER_CONFIRMED, 1L
    )

    private fun door(
        swing: DoorSwing = DoorSwing.SLIDING,
        hinge: HingeEnd? = null,
        type: OpeningType = OpeningType.DOOR,
        offset: Double = 1.0
    ) = Opening(
        "d", "r", type, WallSide.NORTH, offset, 0.9, 2.05, 0.0,
        if (type == OpeningType.DOOR) swing else null,
        hinge, DimensionSource.USER_CONFIRMED, 1L
    )

    private fun issues(vararg items: Furniture, openings: List<Opening> = emptyList()) =
        FurnitureLayoutValidator.validate(room, items.toList(), openings)

    @Test
    fun furnitureInsideTheRoomHasNoIssues() {
        assertTrue(issues(item(x = 2.0, y = 2.0)).isEmpty()) // dodiruje zidove, ali staje
    }

    @Test
    fun furnitureStickingOutOfTheRoomIsReported() {
        assertEquals(listOf(FurnitureIssue.OutsideRoom("f")), issues(item(x = 2.5, y = 0.0)))
        assertEquals(listOf(FurnitureIssue.OutsideRoom("f")), issues(item(x = -0.1, y = 0.0)))
    }

    @Test
    fun rotationChangesWhetherTheFurnitureFits() {
        assertFalse(issues(item(x = 3.0, y = 0.0)).isEmpty())
        assertTrue(issues(item(x = 3.0, y = 0.0, rotation = 90)).isEmpty())
    }

    @Test
    fun furnitureTallerThanTheRoomIsReported() {
        assertEquals(listOf(FurnitureIssue.TooTall("f")), issues(item(x = 0.0, y = 0.0, height = 2.7)))
    }

    @Test
    fun overlappingFurnitureIsReportedButTouchingIsNot() {
        val a = item("a", 0.0, 0.0)
        assertEquals(
            listOf(FurnitureIssue.Overlap("a", "b")),
            issues(a, item("b", 1.0, 0.5))
        )
        assertTrue(issues(a, item("b", 2.0, 0.0)).isEmpty())
    }

    @Test
    fun furnitureInFrontOfADoorBlocksIt() {
        val d = listOf(door())
        val blocked = issues(item(x = 1.2, y = 0.3, length = 0.5, width = 0.5), openings = d)
        assertEquals(listOf(FurnitureIssue.BlocksDoor("f", "d")), blocked)
        assertTrue(issues(item(x = 1.2, y = 0.6, length = 0.5, width = 0.5), openings = d).isEmpty())
        assertTrue(issues(item(x = 2.0, y = 0.0, length = 0.5, width = 0.5), openings = d).isEmpty())
    }

    @Test
    fun furnitureInsideTheSwingOfADoorOpeningIntoTheRoomBlocksIt() {
        val d = listOf(door(DoorSwing.INTO_ROOM, HingeEnd.START))
        // unutar kruga po kome se vrata otvaraju
        assertEquals(
            listOf(FurnitureIssue.BlocksDoor("f", "d")),
            issues(item(x = 1.5, y = 0.7, length = 0.3, width = 0.3), openings = d)
        )
        // u uglu kvadrata luka, ali izvan samog luka
        assertTrue(issues(item(x = 1.8, y = 0.8, length = 0.3, width = 0.3), openings = d).isEmpty())
    }

    @Test
    fun swingOnTheOtherHingeSideIsRespected() {
        val d = listOf(door(DoorSwing.INTO_ROOM, HingeEnd.END))
        assertFalse(issues(item(x = 1.2, y = 0.7, length = 0.3, width = 0.3), openings = d).isEmpty())
        assertTrue(issues(item(x = 1.0, y = 0.7, length = 0.3, width = 0.3), openings = d).isEmpty())
    }

    @Test
    fun doorOpeningOutOfTheRoomDoesNotBlockFurnitureBehindTheEntranceZone() {
        val d = listOf(door(DoorSwing.OUT_OF_ROOM, HingeEnd.START))
        assertTrue(issues(item(x = 1.5, y = 0.7, length = 0.3, width = 0.3), openings = d).isEmpty())
    }

    @Test
    fun windowsNeverBlock() {
        val w = listOf(door(type = OpeningType.WINDOW))
        assertTrue(issues(item(x = 1.2, y = 0.0, length = 0.5, width = 0.5), openings = w).isEmpty())
    }

    @Test
    fun severalProblemsAreAllReported() {
        val result = issues(item(x = 3.5, y = 0.0, height = 3.0))
        assertEquals(setOf<FurnitureIssue>(FurnitureIssue.OutsideRoom("f"), FurnitureIssue.TooTall("f")), result.toSet())
    }

    @Test
    fun firstFreeSpotInAnEmptyRoomIsTheTopLeftCorner() {
        assertEquals(PlanPoint(0.0, 0.0), FurniturePlacement.firstFreeSpot(room, emptyList(), 2.0, 1.0, emptyList()))
    }

    @Test
    fun firstFreeSpotSkipsExistingFurniture() {
        val spot = FurniturePlacement.firstFreeSpot(room, listOf(item(x = 0.0, y = 0.0)), 2.0, 1.0, emptyList())
        assertNotNull(spot)
        assertEquals(2.0, spot!!.xM, 1e-9)
        assertEquals(0.0, spot.yM, 1e-9)
    }

    @Test
    fun firstFreeSpotAvoidsTheDoorPassage() {
        val slidingAtStart = door(offset = 0.0)
        val spot = FurniturePlacement.firstFreeSpot(room, emptyList(), 0.5, 0.5, listOf(slidingAtStart))
        assertNotNull(spot)
        assertEquals(0.9, spot!!.xM, 1e-9)
        assertEquals(0.0, spot.yM, 1e-9)
    }

    @Test
    fun noFreeSpotGivesNull() {
        assertNull(FurniturePlacement.firstFreeSpot(room, emptyList(), 5.0, 1.0, emptyList()))
        val full = item(x = 0.0, y = 0.0, length = 4.0, width = 3.0)
        assertNull(FurniturePlacement.firstFreeSpot(room, listOf(full), 1.0, 1.0, emptyList()))
    }
}
