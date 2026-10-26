package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneStairsTest {

    private fun room(id: String, level: Int, height: Double = 2.6) = Room(
        id = id, projectId = "p", name = id,
        lengthM = 4.0, widthM = 3.0, heightM = height,
        wallThicknessM = 0.2, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = 0.0, yM = 0.0, floorLevel = level
    )

    private fun stairs(direction: Int, lowerLevel: Int = 0) = Stairs(
        id = "s", projectId = "p", lowerLevel = lowerLevel,
        xM = 1.0, yM = 2.0, lengthM = 4.0, widthM = 1.0,
        directionDeg = direction, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L
    )

    @Test
    fun hasOneTreadFewerThanSteps() {
        // visina 1,8 m = 10 stepenika = 9 gazišta
        val boxes = SceneBuilder.stairsBoxes(stairs(0), 0.0, 1.8)
        assertEquals(9, boxes.size)
        assertTrue(boxes.all { it.kind == SceneKind.STAIRS && it.itemId == "s" })
    }

    @Test
    fun treadsRiseOneRiserAtATimeUpToTheLastTread() {
        val boxes = SceneBuilder.stairsBoxes(stairs(0), 1.0, 1.8)
        assertEquals(1.0 + 0.18, boxes.first().max.z, 1e-9)
        assertEquals(1.0 + 0.18 * 9, boxes.last().max.z, 1e-9)
        assertTrue(boxes.all { it.min.z == 1.0 })
    }

    @Test
    fun eastwardStairsStartAtTheWestEndAndFillTheRun() {
        val boxes = SceneBuilder.stairsBoxes(stairs(0), 0.0, 1.8)
        val tread = 4.0 / 9
        assertEquals(1.0, boxes.first().min.x, 1e-9)
        assertEquals(1.0 + tread, boxes.first().max.x, 1e-9)
        assertEquals(5.0, boxes.last().max.x, 1e-9)
        assertEquals(2.0, boxes.first().min.y, 1e-9)
        assertEquals(3.0, boxes.first().max.y, 1e-9)
    }

    @Test
    fun westwardStairsStartAtTheEastEnd() {
        val boxes = SceneBuilder.stairsBoxes(stairs(180), 0.0, 1.8)
        val tread = 4.0 / 9
        assertEquals(5.0 - tread, boxes.first().min.x, 1e-9)
        assertEquals(5.0, boxes.first().max.x, 1e-9)
        assertEquals(1.0, boxes.last().min.x, 1e-9)
    }

    @Test
    fun southwardStairsRunAlongY() {
        // pravac 90: krak ide duž y od 2 do 6, a širina 1 m je duž x od 1 do 2
        val boxes = SceneBuilder.stairsBoxes(stairs(90), 0.0, 1.8)
        assertEquals(2.0, boxes.first().min.y, 1e-9)
        assertEquals(6.0, boxes.last().max.y, 1e-9)
        assertEquals(1.0, boxes.first().min.x, 1e-9)
        assertEquals(2.0, boxes.first().max.x, 1e-9)
    }

    @Test
    fun northwardStairsStartAtTheSouthEnd() {
        val boxes = SceneBuilder.stairsBoxes(stairs(270), 0.0, 1.8)
        val tread = 4.0 / 9
        assertEquals(6.0 - tread, boxes.first().min.y, 1e-9)
        assertEquals(6.0, boxes.first().max.y, 1e-9)
        assertEquals(2.0, boxes.last().min.y, 1e-9)
    }

    @Test
    fun buildPutsStairsOnTheLowerFloorAndBelowTheUpperOne() {
        val rooms = listOf(room("a", 0), room("b", 1))
        val boxes = SceneBuilder.build(rooms, emptyList(), emptyList(), listOf(stairs(0)))
        val steps = boxes.filter { it.kind == SceneKind.STAIRS }
        val upperFloorZ = SceneBuilder.floorOffsets(rooms).getValue(1)
        assertTrue(steps.isNotEmpty())
        assertTrue(steps.all { it.min.z == 0.0 })
        assertTrue(steps.all { it.max.z < upperFloorZ })
    }

    @Test
    fun buildPlacesBasementStairsBelowGroundFloor() {
        val rooms = listOf(room("a", 0), room("b", -1, 2.4))
        val boxes = SceneBuilder.build(rooms, emptyList(), emptyList(), listOf(stairs(0, lowerLevel = -1)))
        val steps = boxes.filter { it.kind == SceneKind.STAIRS }
        val basementZ = SceneBuilder.floorOffsets(rooms).getValue(-1)
        assertTrue(steps.all { it.min.z == basementZ })
        assertTrue(steps.all { it.max.z < 0.0 })
    }

    @Test
    fun buildSkipsStairsWhoseLowerFloorHasNoPlacedRooms() {
        val rooms = listOf(room("a", 0))
        val boxes = SceneBuilder.build(rooms, emptyList(), emptyList(), listOf(stairs(0, lowerLevel = 3)))
        assertTrue(boxes.none { it.kind == SceneKind.STAIRS })
    }

    @Test
    fun buildWithoutStairsHasNoStairsBoxes() {
        val boxes = SceneBuilder.build(listOf(room("a", 0)), emptyList(), emptyList())
        assertTrue(boxes.none { it.kind == SceneKind.STAIRS })
    }

    private fun area(box: SceneBox) = (box.max.x - box.min.x) * (box.max.y - box.min.y)

    private fun rect(x0: Double, y0: Double, x1: Double, y1: Double) = StairsFootprint(x0, y0, x1, y1)

    private fun area(r: StairsFootprint) = (r.x1 - r.x0) * (r.y1 - r.y0)

    @Test
    fun noHolesLeavesTheSlabWhole() {
        val slab = rect(0.0, 0.0, 4.0, 3.0)
        assertEquals(listOf(slab), FloorCutouts.subtract(slab, emptyList()))
    }

    @Test
    fun aHoleInTheMiddleLeavesFourPiecesWithTheRemainingArea() {
        val pieces = FloorCutouts.subtract(rect(0.0, 0.0, 4.0, 3.0), listOf(rect(1.0, 1.0, 3.0, 2.0)))
        assertEquals(4, pieces.size)
        assertEquals(12.0 - 2.0, pieces.sumOf { area(it) }, 1e-9)
    }

    @Test
    fun aHoleInACornerLeavesTwoPieces() {
        val pieces = FloorCutouts.subtract(rect(0.0, 0.0, 4.0, 3.0), listOf(rect(-1.0, -1.0, 2.0, 1.0)))
        assertEquals(2, pieces.size)
        assertEquals(12.0 - 2.0 * 1.0, pieces.sumOf { area(it) }, 1e-9)
    }

    @Test
    fun aHoleCoveringTheWholeSlabLeavesNothing() {
        assertTrue(FloorCutouts.subtract(rect(0.0, 0.0, 4.0, 3.0), listOf(rect(-1.0, -1.0, 5.0, 4.0))).isEmpty())
    }

    @Test
    fun aHoleOutsideTheSlabChangesNothing() {
        val slab = rect(0.0, 0.0, 4.0, 3.0)
        assertEquals(listOf(slab), FloorCutouts.subtract(slab, listOf(rect(10.0, 10.0, 12.0, 11.0))))
    }

    @Test
    fun twoHolesAreBothCut() {
        val pieces = FloorCutouts.subtract(
            rect(0.0, 0.0, 4.0, 3.0),
            listOf(rect(0.5, 0.5, 1.5, 1.5), rect(2.5, 1.0, 3.5, 2.0))
        )
        assertEquals(12.0 - 1.0 - 1.0, pieces.sumOf { area(it) }, 1e-9)
    }

    @Test
    fun theUpperFloorHasAHoleOverTheStairsAndTheLowerFloorIsWhole() {
        val rooms = listOf(room("a", 0), room("b", 1))
        val boxes = SceneBuilder.build(rooms, emptyList(), emptyList(), listOf(stairs(0)))
        val lower = boxes.filter { it.kind == SceneKind.FLOOR && it.label == "a" }
        val upper = boxes.filter { it.kind == SceneKind.FLOOR && it.label == "b" }
        assertEquals(1, lower.size)
        assertEquals(12.0, lower.sumOf { area(it) }, 1e-9)
        // stepenice su 4 x 1 m od x = 1, ali soba (4 x 3 m) se završava na x = 4, pa se seče 3 x 1 m
        assertEquals(12.0 - 3.0, upper.sumOf { area(it) }, 1e-9)
    }

    @Test
    fun withoutStairsTheUpperFloorIsWhole() {
        val rooms = listOf(room("a", 0), room("b", 1))
        val boxes = SceneBuilder.build(rooms, emptyList(), emptyList())
        val upper = boxes.filter { it.kind == SceneKind.FLOOR && it.label == "b" }
        assertEquals(1, upper.size)
    }
}
