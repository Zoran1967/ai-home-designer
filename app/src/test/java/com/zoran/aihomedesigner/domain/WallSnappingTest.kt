package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WallSnappingTest {

    private fun room(
        id: String,
        x: Double?,
        y: Double?,
        length: Double = 4.0,
        width: Double = 3.0,
        thickness: Double? = null
    ) = Room(
        id = id,
        projectId = "p",
        name = id,
        lengthM = length,
        widthM = width,
        heightM = 2.6,
        wallThicknessM = thickness,
        dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L,
        xM = x,
        yM = y
    )

    private fun moved(result: WallSnapping.Result): WallSnapping.Result.Moved {
        assertTrue("Očekivano je pomeranje, a rezultat je $result", result is WallSnapping.Result.Moved)
        return result as WallSnapping.Result.Moved
    }

    @Test
    fun roomToTheRightSnapsToWallThickness() {
        val rooms = listOf(room("a", 0.0, 0.0, thickness = 0.2), room("b", 4.5, 0.0))
        val m = moved(WallSnapping.snap(rooms, "b"))
        assertEquals(4.2, m.position.xM, 1e-9)
        assertEquals(0.0, m.position.yM, 1e-9)
        assertEquals("a", m.neighborId)
        assertEquals(0.2, m.thicknessM, 1e-9)
    }

    @Test
    fun roomToTheLeftSnapsToWallThickness() {
        val rooms = listOf(room("a", 10.0, 0.0, thickness = 0.2), room("b", 5.5, 0.0))
        val m = moved(WallSnapping.snap(rooms, "b"))
        // b je široka 4 m: njena desna ivica treba da bude 0,2 m levo od x = 10
        assertEquals(5.8, m.position.xM, 1e-9)
        assertEquals(0.0, m.position.yM, 1e-9)
    }

    @Test
    fun roomBelowSnapsToWallThickness() {
        val rooms = listOf(room("a", 0.0, 0.0, thickness = 0.2), room("b", 0.0, 3.6))
        val m = moved(WallSnapping.snap(rooms, "b"))
        assertEquals(0.0, m.position.xM, 1e-9)
        assertEquals(3.2, m.position.yM, 1e-9)
    }

    @Test
    fun roomAboveSnapsToWallThickness() {
        val rooms = listOf(room("a", 0.0, 10.0, thickness = 0.2), room("b", 0.0, 6.5))
        val m = moved(WallSnapping.snap(rooms, "b"))
        // b je široka 3 m: njena donja ivica treba da bude 0,2 m iznad y = 10
        assertEquals(6.8, m.position.yM, 1e-9)
    }

    @Test
    fun thicknessIsTheLargerOfTheTwoEnteredValues() {
        val rooms = listOf(
            room("a", 0.0, 0.0, thickness = 0.2),
            room("b", 4.6, 0.0, thickness = 0.25)
        )
        val m = moved(WallSnapping.snap(rooms, "b"))
        assertEquals(0.25, m.thicknessM, 1e-9)
        assertEquals(4.25, m.position.xM, 1e-9)
    }

    @Test
    fun thicknessFromTheMovedRoomAloneIsEnough() {
        val rooms = listOf(room("a", 0.0, 0.0), room("b", 4.6, 0.0, thickness = 0.15))
        val m = moved(WallSnapping.snap(rooms, "b"))
        assertEquals(4.15, m.position.xM, 1e-9)
    }

    @Test
    fun withoutAnyEnteredThicknessTheAppAsksInsteadOfGuessing() {
        val rooms = listOf(room("a", 0.0, 0.0), room("b", 4.5, 0.0))
        assertEquals(WallSnapping.Result.NeedsThickness, WallSnapping.snap(rooms, "b"))
    }

    @Test
    fun farAwayRoomIsNotMoved() {
        val rooms = listOf(room("a", 0.0, 0.0, thickness = 0.2), room("b", 9.0, 0.0))
        assertEquals(WallSnapping.Result.NoNeighborNearby, WallSnapping.snap(rooms, "b"))
    }

    @Test
    fun roomsThatDoNotFaceEachOtherAreNotSnapped() {
        val rooms = listOf(room("a", 0.0, 0.0, thickness = 0.2), room("b", 4.5, 10.0))
        assertEquals(WallSnapping.Result.NoNeighborNearby, WallSnapping.snap(rooms, "b"))
    }

    @Test
    fun thirdRoomInTheWayBlocksTheSnap() {
        val rooms = listOf(
            room("a", 0.0, 0.0, thickness = 0.2),
            room("blocker", 4.2, 0.0, length = 0.3, width = 3.0),
            room("b", 5.0, 0.0)
        )
        // b bi se priljubila uz a, ali tu već stoji treća soba
        val result = WallSnapping.snap(rooms, "b")
        assertTrue(result !is WallSnapping.Result.Moved || result.neighborId != "a")
    }

    @Test
    fun nearestNeighbourWins() {
        val rooms = listOf(
            room("a", 0.0, 0.0, thickness = 0.2),
            room("c", 9.0, 0.0, thickness = 0.2),
            room("b", 4.4, 0.0)
        )
        // do a treba da se pomeri 0,2 m, a do c 0,4 m, pa pobeđuje a
        val m = moved(WallSnapping.snap(rooms, "b"))
        assertEquals("a", m.neighborId)
    }

    @Test
    fun afterSnapTheWallHasExactlyTheChosenThicknessAndNoIssues() {
        val rooms = listOf(room("a", 0.0, 0.0, thickness = 0.2), room("b", 4.55, 0.4))
        val m = moved(WallSnapping.snap(rooms, "b"))
        val after = rooms.map {
            if (it.id == "b") it.copy(xM = m.position.xM, yM = m.position.yM) else it
        }
        val walls = SharedWalls.find(after)
        assertEquals(1, walls.size)
        assertEquals(0.2, walls[0].thicknessM, 1e-9)
        assertTrue(PlanValidator.validate(after).isEmpty())
    }

    @Test
    fun alreadySnappedRoomStaysWhereItIs() {
        val rooms = listOf(room("a", 0.0, 0.0, thickness = 0.2), room("b", 4.2, 0.0))
        val m = moved(WallSnapping.snap(rooms, "b"))
        assertEquals(4.2, m.position.xM, 1e-9)
    }

    @Test
    fun unplacedRoomCannotBeSnapped() {
        val rooms = listOf(room("a", 0.0, 0.0, thickness = 0.2), room("b", null, null))
        assertEquals(WallSnapping.Result.NoNeighborNearby, WallSnapping.snap(rooms, "b"))
    }

    @Test
    fun onlyRoomInProjectHasNoNeighbour() {
        assertEquals(
            WallSnapping.Result.NoNeighborNearby,
            WallSnapping.snap(listOf(room("a", 0.0, 0.0)), "a")
        )
    }
}
