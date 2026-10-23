package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class DepthOrderTest {

    private fun box(
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        kind: SceneKind = SceneKind.FURNITURE,
        id: String = "b"
    ) = SceneBox(Vec3(x0, y0, z0), Vec3(x1, y1, z1), kind, null, id)

    @Test
    fun emptyAndSingleListsAreReturnedAsIs() {
        val eye = Vec3(0.0, 0.0, 10.0)
        assertTrue(DepthOrder.farToNear(emptyList(), eye).isEmpty())
        val one = box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
        assertEquals(listOf(one), DepthOrder.farToNear(listOf(one), eye))
    }

    @Test
    fun theFartherBoxIsDrawnFirstWhateverTheInputOrder() {
        val far = box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, id = "far")
        val near = box(0.0, 5.0, 0.0, 1.0, 6.0, 1.0, id = "near")
        val eye = Vec3(0.5, 20.0, 3.0) // kamera sa juga (veliko y)
        assertEquals(listOf(far, near), DepthOrder.farToNear(listOf(far, near), eye))
        assertEquals(listOf(far, near), DepthOrder.farToNear(listOf(near, far), eye))
    }

    @Test
    fun movingTheCameraToTheOtherSideReversesTheOrder() {
        val a = box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, id = "a")
        val b = box(0.0, 5.0, 0.0, 1.0, 6.0, 1.0, id = "b")
        val eye = Vec3(0.5, -20.0, 3.0) // kamera sa severa
        assertEquals(listOf(b, a), DepthOrder.farToNear(listOf(a, b), eye))
    }

    @Test
    fun smallFurnitureInFrontOfALongWallIsDrawnAfterTheWall() {
        // Središte zida je bliže kameri od središta nameštaja, ali je nameštaj ispred zida.
        // Sortiranje po rastojanju do središta ovde daje pogrešan redosled.
        val wall = box(0.0, 0.0, 0.0, 20.0, 0.2, 2.6, SceneKind.WALL, "wall")
        val chair = box(18.0, 2.0, 0.0, 19.0, 3.0, 1.0, SceneKind.FURNITURE, "chair")
        val eye = Vec3(0.0, 10.0, 5.0)
        assertEquals(listOf(wall, chair), DepthOrder.farToNear(listOf(chair, wall), eye))
        assertEquals(listOf(wall, chair), DepthOrder.farToNear(listOf(wall, chair), eye))
    }

    @Test
    fun furnitureIsDrawnAfterTheFloorWhenSeenFromAbove() {
        val floor = box(0.0, 0.0, -0.05, 10.0, 10.0, 0.0, SceneKind.FLOOR, "floor")
        val table = box(4.0, 4.0, 0.0, 5.0, 5.0, 0.8, SceneKind.FURNITURE, "table")
        val eye = Vec3(5.0, 15.0, 8.0)
        assertEquals(listOf(floor, table), DepthOrder.farToNear(listOf(table, floor), eye))
    }

    @Test
    fun anOpeningIsDrawnAfterTheWallItIsBuiltInto() {
        val wall = box(0.0, 0.0, 0.0, 4.0, 0.2, 2.6, SceneKind.WALL, "wall")
        val door = box(1.0, -0.01, 0.0, 2.0, 0.21, 2.0, SceneKind.DOOR, "door")
        val eye = Vec3(2.0, 10.0, 2.0)
        assertEquals(listOf(wall, door), DepthOrder.farToNear(listOf(door, wall), eye))
        assertEquals(listOf(wall, door), DepthOrder.farToNear(listOf(wall, door), eye))
    }

    @Test
    fun everyBoxIsReturnedExactlyOnceEvenWhenRulesCannotAgree() {
        val random = Random(42)
        val boxes = List(60) { i ->
            val x = random.nextDouble() * 10
            val y = random.nextDouble() * 10
            val z = random.nextDouble() * 5
            box(x, y, z, x + 0.5 + random.nextDouble() * 4, y + 0.5 + random.nextDouble() * 4,
                z + 0.5 + random.nextDouble() * 2, id = "r$i")
        }
        for (eye in listOf(Vec3(5.0, 30.0, 12.0), Vec3(-20.0, 5.0, 3.0), Vec3(5.0, 5.0, 40.0))) {
            val ordered = DepthOrder.farToNear(boxes, eye)
            assertEquals(boxes.size, ordered.size)
            assertEquals(boxes.map { it.itemId }.toSet(), ordered.map { it.itemId }.toSet())
            // isti objekti, nijedan nije prepisan
            boxes.forEach { original -> assertTrue(ordered.any { it === original }) }
        }
    }

    @Test
    fun theInputListIsNotModified() {
        val a = box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0, id = "a")
        val b = box(0.0, 5.0, 0.0, 1.0, 6.0, 1.0, id = "b")
        val input = listOf(b, a)
        DepthOrder.farToNear(input, Vec3(0.5, -20.0, 3.0))
        assertSame(b, input[0])
        assertSame(a, input[1])
    }
}
