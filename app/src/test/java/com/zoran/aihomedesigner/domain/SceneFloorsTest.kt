package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneFloorsTest {

    private fun room(id: String, level: Int, height: Double = 2.6) = Room(
        id = id, projectId = "p", name = id,
        lengthM = 4.0, widthM = 3.0, heightM = height,
        wallThicknessM = 0.2, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = 0.0, yM = 0.0, floorLevel = level
    )

    @Test
    fun singleGroundFloorStartsAtZero() {
        assertEquals(mapOf(0 to 0.0), SceneBuilder.floorOffsets(listOf(room("a", 0))))
    }

    @Test
    fun upperFloorStartsAboveTheFloorBelowPlusTheSlab() {
        val offsets = SceneBuilder.floorOffsets(listOf(room("a", 0, 2.6), room("b", 1, 2.5)))
        assertEquals(0.0, offsets.getValue(0), 1e-9)
        assertEquals(2.6 + SceneBuilder.FLOOR_SLAB_M, offsets.getValue(1), 1e-9)
    }

    @Test
    fun theTallestRoomOfAFloorDecidesItsHeight() {
        val offsets = SceneBuilder.floorOffsets(listOf(room("a", 0, 2.4), room("b", 0, 3.0), room("c", 1)))
        assertEquals(3.0 + SceneBuilder.FLOOR_SLAB_M, offsets.getValue(1), 1e-9)
    }

    @Test
    fun aMissingFloorBetweenTwoFloorsUsesTheDefaultHeight() {
        val offsets = SceneBuilder.floorOffsets(listOf(room("a", 0), room("c", 2)))
        val first = 2.6 + SceneBuilder.FLOOR_SLAB_M
        assertEquals(first, offsets.getValue(1), 1e-9)
        assertEquals(first + SceneBuilder.DEFAULT_FLOOR_HEIGHT_M + SceneBuilder.FLOOR_SLAB_M, offsets.getValue(2), 1e-9)
    }

    @Test
    fun aBasementIsBelowZeroByItsOwnHeightPlusTheSlab() {
        val offsets = SceneBuilder.floorOffsets(listOf(room("a", 0), room("b", -1, 2.4)))
        assertEquals(-(2.4 + SceneBuilder.FLOOR_SLAB_M), offsets.getValue(-1), 1e-9)
    }

    @Test
    fun roomsOnDifferentFloorsAreStackedInTheScene() {
        val boxes = SceneBuilder.build(listOf(room("a", 0), room("b", 1)), emptyList(), emptyList())
        val floors = boxes.filter { it.kind == SceneKind.FLOOR }
        val upper = floors.first { it.label == "b" }
        val lower = floors.first { it.label == "a" }
        assertEquals(2.6 + SceneBuilder.FLOOR_SLAB_M, upper.max.z, 1e-9)
        assertEquals(0.0, lower.max.z, 1e-9)
        val upperWalls = boxes.filter { it.kind == SceneKind.WALL && it.label == "b" }
        assertTrue(upperWalls.all { it.min.z >= 2.6 + SceneBuilder.FLOOR_SLAB_M - 1e-9 })
    }

    @Test
    fun furnitureAndOpeningsFollowTheirRoomsFloor() {
        val bed = Furniture(
            "f", "b", "Krevet", FurnitureCategory.BED, 2.0, 1.0, 0.8,
            0.5, 0.5, 0, DimensionSource.USER_CONFIRMED, 1L
        )
        val door = Opening(
            "o", "b", OpeningType.DOOR, WallSide.NORTH, 1.0, 0.9, 2.05, 0.0,
            DoorSwing.SLIDING, null, DimensionSource.USER_CONFIRMED, 1L
        )
        val boxes = SceneBuilder.build(listOf(room("a", 0), room("b", 1)), listOf(door), listOf(bed))
        val floorZ = 2.6 + SceneBuilder.FLOOR_SLAB_M
        val furnitureBox = boxes.first { it.kind == SceneKind.FURNITURE }
        assertEquals(floorZ, furnitureBox.min.z, 1e-9)
        assertEquals(floorZ + 0.8, furnitureBox.max.z, 1e-9)
        val doorBox = boxes.first { it.kind == SceneKind.DOOR }
        assertEquals(floorZ, doorBox.min.z, 1e-9)
        assertEquals(floorZ + 2.05, doorBox.max.z, 1e-9)
    }

    @Test
    fun aWindowSillIsMeasuredFromItsOwnFloor() {
        val window = Opening(
            "w", "b", OpeningType.WINDOW, WallSide.EAST, 0.5, 1.2, 1.2, 0.9,
            null, null, DimensionSource.USER_CONFIRMED, 1L
        )
        val boxes = SceneBuilder.build(listOf(room("a", 0), room("b", 1)), listOf(window), emptyList())
        val floorZ = 2.6 + SceneBuilder.FLOOR_SLAB_M
        val box = boxes.first { it.kind == SceneKind.WINDOW }
        assertEquals(floorZ + 0.9, box.min.z, 1e-9)
        assertEquals(floorZ + 2.1, box.max.z, 1e-9)
    }
}
