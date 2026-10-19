package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FurnitureCheckerTest {

    // Soba 4 m (x) x 3 m (y), visine 2,6 m, gornji levi ugao na planu u (0, 0)
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

    private fun item(
        id: String,
        x: Double,
        y: Double,
        length: Double = 0.5,
        width: Double = 0.5,
        height: Double = 0.8,
        turns: Int = 0
    ) = Furniture(
        id = id,
        roomId = "r",
        name = id,
        category = FurnitureCategory.OTHER,
        lengthM = length,
        widthM = width,
        heightM = height,
        xM = x,
        yM = y,
        quarterTurns = turns,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L
    )

    private fun door(
        side: WallSide = WallSide.NORTH,
        offset: Double = 1.0,
        width: Double = 0.9,
        swing: DoorSwing = DoorSwing.INTO_ROOM,
        hinge: HingeEnd? = HingeEnd.START
    ) = Opening(
        id = "d",
        roomId = "r",
        type = OpeningType.DOOR,
        side = side,
        offsetM = offset,
        widthM = width,
        heightM = 2.05,
        sillHeightM = 0.0,
        swing = swing,
        hinge = if (swing == DoorSwing.SLIDING) null else hinge,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L
    )

    private fun window() = Opening(
        id = "w",
        roomId = "r",
        type = OpeningType.WINDOW,
        side = WallSide.NORTH,
        offsetM = 1.0,
        widthM = 1.2,
        heightM = 1.2,
        sillHeightM = 0.9,
        swing = null,
        hinge = null,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L
    )

    private fun check(furniture: List<Furniture>, openings: List<Opening> = emptyList()) =
        FurnitureChecker.check(furniture, room, openings)

    @Test
    fun furnitureInsideTheRoomHasNoIssues() {
        assertTrue(check(listOf(item("a", 2.0, 1.0))).isEmpty())
    }

    @Test
    fun furnitureTouchingTheWallsIsAllowed() {
        assertTrue(check(listOf(item("a", 0.0, 0.0), item("b", 3.5, 2.5))).isEmpty())
    }

    @Test
    fun furnitureSticksOutOfTheRoom() {
        assertEquals(listOf(FurnitureIssue.OutsideRoom("a")), check(listOf(item("a", 3.8, 1.0))))
        assertEquals(listOf(FurnitureIssue.OutsideRoom("a")), check(listOf(item("a", -0.1, 1.0))))
        assertEquals(listOf(FurnitureIssue.OutsideRoom("a")), check(listOf(item("a", 1.0, 2.8))))
    }

    @Test
    fun turnedFurnitureUsesTheTurnedFootprint() {
        // 2 x 0,5 okrenut za 90 stepeni zauzima 0,5 duž x i 2 duž y; na y = 1,5 izlazi (1,5 + 2 > 3)
        val long = item("a", 1.0, 1.5, length = 2.0, width = 0.5, turns = 1)
        assertEquals(listOf(FurnitureIssue.OutsideRoom("a")), check(listOf(long)))
        // bez okretanja stoji u sobi
        assertTrue(check(listOf(long.copy(quarterTurns = 0))).isEmpty())
    }

    @Test
    fun furnitureTallerThanTheRoomIsReported() {
        assertEquals(listOf(FurnitureIssue.TooTall("a")), check(listOf(item("a", 2.0, 1.0, height = 2.7))))
    }

    @Test
    fun overlappingFurnitureIsReported() {
        val issues = check(listOf(item("a", 1.0, 1.0), item("b", 1.3, 1.3)))
        assertEquals(listOf(FurnitureIssue.OverlapsFurniture("a", "b")), issues)
    }

    @Test
    fun furnitureThatOnlyTouchesIsAllowed() {
        assertTrue(check(listOf(item("a", 1.0, 1.0), item("b", 1.5, 1.0))).isEmpty())
    }

    @Test
    fun furnitureInTheSwingOfADoorOpeningIntoTheRoomIsReported() {
        // vrata na severnom zidu, šarke u (1, 0), list ide do (1, 0,9)
        val issues = check(listOf(item("a", 1.0, 0.0)), listOf(door()))
        assertTrue(issues.contains(FurnitureIssue.BlocksDoorSwing("a", "d")))
    }

    @Test
    fun furnitureInsideTheBoundingSquareButOutsideTheArcIsNotInTheSwing() {
        // 0,5 x 0,5 na (1,8 ; 0,7): ugao najbliži šarkama je na rastojanju oko 1,06 m, a radijus je 0,9 m
        val issues = check(listOf(item("a", 1.8, 0.7)), listOf(door()))
        assertTrue(issues.isEmpty())
    }

    @Test
    fun furnitureBehindTheHingeSideOfTheDoorIsNotInTheSwing() {
        val issues = check(listOf(item("a", 0.2, 0.0)), listOf(door()))
        assertTrue(issues.isEmpty())
    }

    @Test
    fun hingeAtTheOtherEndMovesTheSwingArea() {
        val atEnd = door(hinge = HingeEnd.END) // šarke u (1,9 ; 0), list ide do (1,9 ; 0,9)
        val nearFirstEnd = check(listOf(item("a", 1.0, 0.2, length = 0.1, width = 0.1)), listOf(atEnd))
        assertTrue(nearFirstEnd.any { it is FurnitureIssue.BlocksDoorSwing })
        val nearHinge = check(listOf(item("b", 2.0, 0.0)), listOf(atEnd))
        assertTrue(nearHinge.isEmpty())
    }

    @Test
    fun furnitureInFrontOfADoorBlocksTheDoorway() {
        // vrata se otvaraju napolje, pa nema prostora otvaranja u sobi, ali ispred njih mora biti prolaz
        val outward = door(swing = DoorSwing.OUT_OF_ROOM)
        val issues = check(listOf(item("a", 1.2, 0.3)), listOf(outward))
        assertEquals(listOf(FurnitureIssue.BlocksDoorway("a", "d")), issues)
    }

    @Test
    fun furnitureJustBeyondTheDoorwayDepthIsAllowed() {
        val outward = door(swing = DoorSwing.OUT_OF_ROOM)
        assertTrue(check(listOf(item("a", 1.2, FurnitureChecker.DOOR_CLEARANCE_M)), listOf(outward)).isEmpty())
    }

    @Test
    fun slidingDoorStillNeedsAClearDoorway() {
        val sliding = door(swing = DoorSwing.SLIDING)
        val issues = check(listOf(item("a", 1.2, 0.2)), listOf(sliding))
        assertEquals(listOf(FurnitureIssue.BlocksDoorway("a", "d")), issues)
    }

    @Test
    fun windowsDoNotBlockFurniture() {
        assertTrue(check(listOf(item("a", 1.2, 0.0)), listOf(window())).isEmpty())
    }

    @Test
    fun doorOnTheSouthWallUsesTheSouthEdge() {
        val south = door(side = WallSide.SOUTH, swing = DoorSwing.OUT_OF_ROOM)
        val blocked = check(listOf(item("a", 1.2, 2.3)), listOf(south))
        assertEquals(listOf(FurnitureIssue.BlocksDoorway("a", "d")), blocked)
        assertTrue(check(listOf(item("b", 1.2, 1.0)), listOf(south)).isEmpty())
    }

    @Test
    fun doorOnTheEastWallUsesTheEastEdge() {
        val east = door(side = WallSide.EAST, swing = DoorSwing.INTO_ROOM, hinge = HingeEnd.START)
        // šarke u (4 ; 1), list ide do (3,1 ; 1)
        val blocked = check(listOf(item("a", 3.5, 1.0)), listOf(east))
        assertTrue(blocked.any { it is FurnitureIssue.BlocksDoorSwing })
    }

    @Test
    fun findFreeSpotStartsInTheCornerWhenNothingIsInTheWay() {
        val spot = FurnitureChecker.findFreeSpot(room, emptyList(), emptyList(), 0.5, 0.5)
        assertNotNull(spot)
        assertEquals(0.0, spot!!.xM, 1e-9)
        assertEquals(0.0, spot.yM, 1e-9)
    }

    @Test
    fun findFreeSpotSkipsTheDoorArea() {
        val atCorner = door(offset = 0.0) // šarke u (0, 0), prostor otvaranja 0,9 x 0,9, prolaz 0,9 x 0,6
        val spot = FurnitureChecker.findFreeSpot(room, emptyList(), listOf(atCorner), 0.5, 0.5)
        assertNotNull(spot)
        assertEquals(0.9, spot!!.xM, 1e-6)
        assertEquals(0.0, spot.yM, 1e-6)
    }

    @Test
    fun findFreeSpotSkipsOtherFurniture() {
        val others = listOf(item("a", 0.0, 0.0, length = 1.0, width = 1.0))
        val spot = FurnitureChecker.findFreeSpot(room, others, emptyList(), 0.5, 0.5)
        assertEquals(1.0, spot!!.xM, 1e-6)
        assertEquals(0.0, spot.yM, 1e-6)
    }

    @Test
    fun findFreeSpotReturnsNullWhenTheRoomIsFull() {
        val others = listOf(item("a", 0.0, 0.0, length = 4.0, width = 3.0))
        assertNull(FurnitureChecker.findFreeSpot(room, others, emptyList(), 0.5, 0.5))
    }

    @Test
    fun findFreeSpotReturnsNullWhenTheObjectIsBiggerThanTheRoom() {
        assertNull(FurnitureChecker.findFreeSpot(room, emptyList(), emptyList(), 5.0, 1.0))
    }

    @Test
    fun foundSpotNeverProducesIssuesWhenChecked() {
        val openings = listOf(door(offset = 0.0), door(side = WallSide.EAST, offset = 1.0))
        val others = listOf(item("a", 0.0, 1.0, length = 1.0, width = 1.0))
        val spot = FurnitureChecker.findFreeSpot(room, others, openings, 0.8, 0.6)!!
        val placed = item("new", spot.xM, spot.yM, length = 0.8, width = 0.6)
        assertTrue(check(others + placed, openings).isEmpty())
    }
}
