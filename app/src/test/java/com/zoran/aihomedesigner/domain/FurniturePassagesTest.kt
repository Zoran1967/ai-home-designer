package com.zoran.aihomedesigner.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FurniturePassagesTest {

    private fun item(
        id: String,
        x: Double,
        y: Double,
        length: Double = 1.0,
        width: Double = 1.0,
        category: FurnitureCategory = FurnitureCategory.OTHER
    ) = Furniture(
        id, "r", id, category, length, width, 0.8,
        x, y, 0, DimensionSource.USER_CONFIRMED, 1L
    )

    @Test
    fun gapBetweenFacingPiecesUnderSixtyCentimetresIsReported() {
        val gaps = FurniturePassages.narrowGaps(listOf(item("a", 0.0, 0.0), item("b", 1.4, 0.0)))
        assertEquals(listOf(NarrowGap("a", "b", 0.4)), gaps)
    }

    @Test
    fun verticalGapIsReportedToo() {
        val gaps = FurniturePassages.narrowGaps(listOf(item("a", 0.0, 0.0), item("b", 0.0, 1.5)))
        assertEquals(1, gaps.size)
        assertEquals(0.5, gaps[0].gapM, 1e-9)
    }

    @Test
    fun gapOfSixtyCentimetresOrMoreIsFine() {
        assertTrue(FurniturePassages.narrowGaps(listOf(item("a", 0.0, 0.0), item("b", 1.6, 0.0))).isEmpty())
        assertTrue(FurniturePassages.narrowGaps(listOf(item("a", 0.0, 0.0), item("b", 3.0, 0.0))).isEmpty())
    }

    @Test
    fun piecesStandingAgainstEachOtherAreNotAGap() {
        assertTrue(FurniturePassages.narrowGaps(listOf(item("a", 0.0, 0.0), item("b", 1.0, 0.0))).isEmpty())
        assertTrue(FurniturePassages.narrowGaps(listOf(item("a", 0.0, 0.0), item("b", 1.01, 0.0))).isEmpty())
    }

    @Test
    fun overlappingPiecesAreLeftToTheOverlapCheck() {
        assertTrue(FurniturePassages.narrowGaps(listOf(item("a", 0.0, 0.0), item("b", 0.5, 0.0))).isEmpty())
    }

    @Test
    fun piecesThatOnlyMeetDiagonallyAreIgnored() {
        assertTrue(FurniturePassages.narrowGaps(listOf(item("a", 0.0, 0.0), item("b", 1.3, 1.3))).isEmpty())
    }

    @Test
    fun piecesFacingEachOtherOnlyBriefAreIgnored() {
        // preklapanje duž stranice je samo 20 cm
        assertTrue(FurniturePassages.narrowGaps(listOf(item("a", 0.0, 0.0), item("b", 1.4, 0.8))).isEmpty())
    }

    @Test
    fun chairNextToATableOrDeskIsNotReported() {
        val table = item("t", 0.0, 0.0, category = FurnitureCategory.TABLE)
        val desk = item("d", 0.0, 3.0, category = FurnitureCategory.DESK)
        val chair = item("c", 1.3, 0.0, category = FurnitureCategory.CHAIR)
        assertTrue(FurniturePassages.narrowGaps(listOf(table, chair)).isEmpty())
        assertTrue(FurniturePassages.narrowGaps(listOf(chair.copy(xM = 0.0, yM = 4.3), desk)).isEmpty())
    }

    @Test
    fun chairNextToASofaIsReported() {
        val sofa = item("s", 0.0, 0.0, category = FurnitureCategory.SOFA)
        val chair = item("c", 1.3, 0.0, category = FurnitureCategory.CHAIR)
        assertEquals(1, FurniturePassages.narrowGaps(listOf(sofa, chair)).size)
    }

    @Test
    fun orderOfThePiecesDoesNotChangeTheGap() {
        val a = item("a", 0.0, 0.0)
        val b = item("b", 1.4, 0.0)
        assertEquals(0.4, FurniturePassages.narrowGaps(listOf(b, a))[0].gapM, 1e-9)
    }

    @Test
    fun rotatedPiecesUseTheirRotatedFootprint() {
        // a je 2 x 1, okrenut za 90 stepeni zauzima 1 x 2; b je 0,4 desno od njega
        val a = item("a", 0.0, 0.0, length = 2.0, width = 1.0).copy(rotationDeg = 90)
        val b = item("b", 1.4, 0.0)
        assertEquals(0.4, FurniturePassages.narrowGaps(listOf(a, b))[0].gapM, 1e-9)
    }
}
