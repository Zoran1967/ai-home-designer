package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Scene3DTest {

    private fun room(
        x: Double? = 2.0,
        y: Double? = 1.0,
        thickness: Double? = 0.2,
        id: String = "r"
    ) = Room(
        id = id, projectId = "p", name = "Soba",
        lengthM = 4.0, widthM = 3.0, heightM = 2.6,
        wallThicknessM = thickness, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = x, yM = y
    )

    private fun assertVec(x: Double, y: Double, z: Double, v: Vec3) {
        assertEquals(x, v.x, 1e-9)
        assertEquals(y, v.y, 1e-9)
        assertEquals(z, v.z, 1e-9)
    }

    @Test
    fun oneRoomGivesAFloorAndFourWalls() {
        val boxes = SceneBuilder.build(listOf(room()), emptyList(), emptyList())
        assertEquals(1, boxes.count { it.kind == SceneKind.FLOOR })
        assertEquals(4, boxes.count { it.kind == SceneKind.WALL })
    }

    @Test
    fun floorAndWallsSurroundTheInteriorUsingTheEnteredThickness() {
        val boxes = SceneBuilder.build(listOf(room()), emptyList(), emptyList())
        val floor = boxes.first { it.kind == SceneKind.FLOOR }
        assertVec(2.0, 1.0, -SceneBuilder.SLAB_M, floor.min)
        assertVec(6.0, 4.0, 0.0, floor.max)

        val walls = boxes.filter { it.kind == SceneKind.WALL }
        // severni zid
        assertVec(1.8, 0.8, 0.0, walls[0].min)
        assertVec(6.2, 1.0, 2.6, walls[0].max)
        // južni zid
        assertVec(1.8, 4.0, 0.0, walls[1].min)
        assertVec(6.2, 4.2, 2.6, walls[1].max)
        // zapadni zid
        assertVec(1.8, 1.0, 0.0, walls[2].min)
        assertVec(2.0, 4.0, 2.6, walls[2].max)
        // istočni zid
        assertVec(6.0, 1.0, 0.0, walls[3].min)
        assertVec(6.2, 4.0, 2.6, walls[3].max)
    }

    @Test
    fun missingWallThicknessIsDrawnWithTheVisualDefault() {
        val boxes = SceneBuilder.build(listOf(room(thickness = null)), emptyList(), emptyList())
        val north = boxes.filter { it.kind == SceneKind.WALL }[0]
        assertEquals(SceneBuilder.VISUAL_WALL_M, north.max.y - north.min.y, 1e-9)
    }

    @Test
    fun roomsWithoutAPositionAreNotDrawn() {
        assertTrue(SceneBuilder.build(listOf(room(x = null, y = null)), emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun furnitureIsPlacedRelativeToItsRoomAndUsesItsRotatedFootprint() {
        val bed = Furniture(
            "f", "r", "Krevet", FurnitureCategory.BED, 2.0, 1.0, 0.8,
            0.5, 0.25, 90, DimensionSource.USER_CONFIRMED, 1L
        )
        val box = SceneBuilder.build(listOf(room()), emptyList(), listOf(bed)).first { it.kind == SceneKind.FURNITURE }
        assertVec(2.5, 1.25, 0.0, box.min)
        assertVec(3.5, 3.25, 0.8, box.max)
        assertEquals("Krevet", box.label)
    }

    @Test
    fun furnitureOfAnUnplacedRoomIsSkipped() {
        val bed = Furniture(
            "f", "r", "Krevet", FurnitureCategory.BED, 2.0, 1.0, 0.8,
            0.0, 0.0, 0, DimensionSource.USER_CONFIRMED, 1L
        )
        val boxes = SceneBuilder.build(listOf(room(x = null, y = null)), emptyList(), listOf(bed))
        assertTrue(boxes.none { it.kind == SceneKind.FURNITURE })
    }

    private fun opening(type: OpeningType, side: WallSide, offset: Double, width: Double, height: Double, sill: Double) =
        Opening(
            "o", "r", type, side, offset, width, height, sill,
            if (type == OpeningType.DOOR) DoorSwing.INTO_ROOM else null,
            if (type == OpeningType.DOOR) HingeEnd.START else null,
            DimensionSource.USER_CONFIRMED, 1L
        )

    @Test
    fun doorIsAPanelThroughTheNorthWallFromTheFloorUp() {
        val door = opening(OpeningType.DOOR, WallSide.NORTH, 1.0, 0.9, 2.05, 0.0)
        val box = SceneBuilder.build(listOf(room()), listOf(door), emptyList()).first { it.kind == SceneKind.DOOR }
        assertVec(3.0, 1.0 - 0.2 - SceneBuilder.OPENING_PROUD_M, 0.0, box.min)
        assertVec(3.9, 1.0 + SceneBuilder.OPENING_PROUD_M, 2.05, box.max)
    }

    @Test
    fun windowIsAPanelThroughTheEastWallAtTheSillHeight() {
        val window = opening(OpeningType.WINDOW, WallSide.EAST, 0.5, 1.2, 1.2, 0.9)
        val box = SceneBuilder.build(listOf(room()), listOf(window), emptyList()).first { it.kind == SceneKind.WINDOW }
        assertVec(6.0 - SceneBuilder.OPENING_PROUD_M, 1.5, 0.9, box.min)
        assertVec(6.2 + SceneBuilder.OPENING_PROUD_M, 2.7, 2.1, box.max)
    }

    @Test
    fun openingsOfAnUnplacedRoomAreSkipped() {
        val door = opening(OpeningType.DOOR, WallSide.NORTH, 1.0, 0.9, 2.05, 0.0)
        val boxes = SceneBuilder.build(listOf(room(x = null, y = null)), listOf(door), emptyList())
        assertTrue(boxes.none { it.kind == SceneKind.DOOR })
    }

    // ---------- Projekcija ----------

    private val camera = Camera3D(yawDeg = 0.0, pitchDeg = 0.0, distanceM = 10.0, target = Vec3(0.0, 0.0, 0.0))

    @Test
    fun theTargetProjectsToTheCentreOfTheScreen() {
        val p = Projection.project(Vec3(0.0, 0.0, 0.0), camera, 800.0, 600.0)!!
        assertEquals(400.0, p.x, 1e-6)
        assertEquals(300.0, p.y, 1e-6)
        assertEquals(10.0, p.depth, 1e-6)
    }

    @Test
    fun eastIsRightAndUpIsUpWhenLookingNorth() {
        val east = Projection.project(Vec3(1.0, 0.0, 0.0), camera, 800.0, 600.0)!!
        val up = Projection.project(Vec3(0.0, 0.0, 1.0), camera, 800.0, 600.0)!!
        assertTrue(east.x > 400.0)
        assertEquals(300.0, east.y, 1e-6)
        assertTrue(up.y < 300.0)
        assertEquals(400.0, up.x, 1e-6)
    }

    @Test
    fun lookingFromTheNorthSwapsLeftAndRight() {
        val fromNorth = camera.copy(yawDeg = 180.0)
        val east = Projection.project(Vec3(1.0, 0.0, 0.0), fromNorth, 800.0, 600.0)!!
        assertTrue(east.x < 400.0)
    }

    @Test
    fun nearerPointsAreLargerAndHaveSmallerDepth() {
        val near = Projection.project(Vec3(1.0, 5.0, 0.0), camera, 800.0, 600.0)!!   // bliže kameri (kamera je na jugu)
        val far = Projection.project(Vec3(1.0, -5.0, 0.0), camera, 800.0, 600.0)!!
        assertTrue(near.depth < far.depth)
        assertTrue(near.x - 400.0 > far.x - 400.0)
    }

    @Test
    fun pointsBehindTheCameraAreNotProjected() {
        assertNull(Projection.project(Vec3(0.0, 20.0, 0.0), camera, 800.0, 600.0))
    }

    @Test
    fun lookingStraightDownStillGivesAUsableView() {
        val top = camera.copy(pitchDeg = 90.0)
        val centre = Projection.project(Vec3(0.0, 0.0, 0.0), top, 800.0, 600.0)
        val north = Projection.project(Vec3(0.0, -1.0, 0.0), top, 800.0, 600.0)
        assertNotNull(centre)
        assertNotNull(north)
        assertTrue(north!!.y < centre!!.y) // sever je gore na ekranu
    }
}
