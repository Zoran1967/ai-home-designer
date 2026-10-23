package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FurniturePassagesTest {

    // Soba 4 m (x) x 3 m (y)
    private val room = Room(
        id = "r", projectId = "p", name = "Soba",
        lengthM = 4.0, widthM = 3.0, heightM = 2.6,
        wallThicknessM = null, dimensionSource = DimensionSource.USER_CONFIRMED,
        createdAtMillis = 1L, xM = 0.0, yM = 0.0
    )

    private fun item(id: String, x: Double, y: Double, length: Double = 1.0, width: Double = 1.0) = Furniture(
        id, "r", id, FurnitureCategory.OTHER, length, width, 0.8,
        x, y, 0, DimensionSource.USER_CONFIRMED, 1L
    )

    private fun find(vararg items: Furniture) = FurniturePassages.find(room, items.toList())

    @Test
    fun gapBetweenTwoItemsBelowTheRecommendationIsReported() {
        val result = find(item("a", 0.0, 1.0), item("b", 1.4, 1.0))
        assertEquals(1, result.count { it.otherFurnitureId != null })
        val passage = result.first { it.otherFurnitureId != null }
        assertEquals(0.4, passage.gapM, 1e-9)
        assertEquals("a", passage.furnitureId)
        assertEquals("b", passage.otherFurnitureId)
    }

    @Test
    fun gapOfExactlyTheRecommendationIsFine() {
        val result = find(item("a", 0.0, 1.0), item("b", 1.6, 1.0))
        assertTrue(result.none { it.otherFurnitureId != null })
    }

    @Test
    fun touchingItemsAndVerySmallGapsAreNotReported() {
        assertTrue(find(item("a", 0.0, 1.0), item("b", 1.0, 1.0)).none { it.otherFurnitureId != null })
        assertTrue(find(item("a", 0.0, 1.0), item("b", 1.1, 1.0)).none { it.otherFurnitureId != null })
    }

    @Test
    fun itemsThatBarelyFaceEachOtherAreNotReported() {
        // preklapaju se samo 0,3 m po visini
        assertTrue(find(item("a", 0.0, 0.0), item("b", 1.4, 0.7)).none { it.otherFurnitureId != null })
    }

    @Test
    fun verticalGapsAreFoundToo() {
        val result = find(item("a", 1.0, 0.0), item("b", 1.0, 1.4))
        assertEquals(0.4, result.first { it.otherFurnitureId != null }.gapM, 1e-9)
    }

    @Test
    fun overlappingItemsAreLeftToTheOverlapCheck() {
        assertTrue(find(item("a", 0.0, 1.0), item("b", 0.5, 1.0)).none { it.otherFurnitureId != null })
    }

    @Test
    fun gapToAWallIsReportedPerSide() {
        val west = find(item("a", 0.4, 1.0)).first { it.wall != null }
        assertEquals(WallSide.WEST, west.wall)
        assertEquals(0.4, west.gapM, 1e-9)

        val east = find(item("a", 2.5, 1.0)).first { it.wall == WallSide.EAST }
        assertEquals(0.5, east.gapM, 1e-9)

        val north = find(item("a", 1.0, 0.3)).first { it.wall == WallSide.NORTH }
        assertEquals(0.3, north.gapM, 1e-9)

        val south = find(item("a", 1.0, 1.7)).first { it.wall == WallSide.SOUTH }
        assertEquals(0.3, south.gapM, 1e-9)
    }

    @Test
    fun itemAgainstAWallOrVeryCloseToItIsNotReported() {
        assertTrue(find(item("a", 0.0, 0.0)).none { it.wall == WallSide.WEST || it.wall == WallSide.NORTH })
        assertTrue(find(item("a", 0.1, 1.0)).none { it.wall == WallSide.WEST })
    }

    @Test
    fun itemShorterThanTheFacingLengthAlongTheWallIsNotReported() {
        // širina 0,3 m duž zapadnog zida
        assertTrue(find(item("a", 0.4, 1.0, length = 1.0, width = 0.3)).none { it.wall == WallSide.WEST })
    }

    @Test
    fun emptyRoomHasNoNarrowPassages() {
        assertTrue(find().isEmpty())
    }
}
