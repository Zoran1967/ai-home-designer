package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StairsIssueIdsTest {

    private fun room(id: String, level: Int = 0, x: Double = 0.0, y: Double = 0.0) = Room(
        id = id, projectId = "p", name = id, lengthM = 6.0, widthM = 4.0, heightM = 2.6,
        wallThicknessM = null, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = x, yM = y, floorLevel = level
    )

    private fun stairs(id: String, x: Double, y: Double, lower: Int = 0) = Stairs(
        id = id, projectId = "p", lowerLevel = lower, xM = x, yM = y,
        lengthM = 4.0, widthM = 1.0, directionDeg = 0,
        dimensionSource = DimensionSource.USER_CONFIRMED, createdAtMillis = 1L
    )

    @Test
    fun noStairsMeansNoIds() {
        assertTrue(StairsLayoutValidator.idsWithIssues(emptyList(), listOf(room("a"))).isEmpty())
    }

    @Test
    fun onlyTheStairsWithAProblemAreListed() {
        val inside = stairs("ok", 1.0, 1.0)
        val poking = stairs("bad", 3.0, 3.0) // x od 3 do 7, a soba se završava na 6 (y od 3 do 4 se ne preklapa sa "ok")
        val ids = StairsLayoutValidator.idsWithIssues(listOf(inside, poking), listOf(room("a")))
        assertEquals(setOf("bad"), ids)
    }

    @Test
    fun bothStairsOfAnOverlappingPairAreListed() {
        val a = stairs("a", 1.0, 1.0)
        val b = stairs("b", 2.0, 1.5)
        val ids = StairsLayoutValidator.idsWithIssues(listOf(a, b), listOf(room("r")))
        assertEquals(setOf("a", "b"), ids)
    }

    @Test
    fun stairsOnAFloorWithoutRoomsAreNotFlagged() {
        val ids = StairsLayoutValidator.idsWithIssues(listOf(stairs("s", 1.0, 1.0)), emptyList())
        assertTrue(ids.isEmpty())
    }
}
