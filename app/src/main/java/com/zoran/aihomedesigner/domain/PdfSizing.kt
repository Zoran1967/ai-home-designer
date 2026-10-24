package com.zoran.aihomedesigner.domain

import kotlin.math.max
import kotlin.math.roundToInt

/** Veličina slike u pikselima. */
data class PixelSize(val widthPx: Int, val heightPx: Int)

object PdfSizing {

    /** Duža strana slike dobijene iz strane PDF-a (3000 piksela): dovoljno oštro za obeležavanje tačaka. */
    const val LONG_SIDE_PX = 3000

    /**
     * Veličina slike u koju se iscrtava strana PDF-a. Strana ima [pageWidthPt] x [pageHeightPt] tačaka
     * (1 tačka = 1/72 inča). Odnos strana ostaje isti, duža strana je [longSidePx] piksela.
     * Vraća null ako strana nema ispravne dimenzije.
     */
    fun sizeFor(pageWidthPt: Int, pageHeightPt: Int, longSidePx: Int = LONG_SIDE_PX): PixelSize? {
        if (pageWidthPt <= 0 || pageHeightPt <= 0 || longSidePx <= 0) return null
        val scale = longSidePx.toDouble() / max(pageWidthPt, pageHeightPt)
        return PixelSize(
            widthPx = (pageWidthPt * scale).roundToInt().coerceAtLeast(1),
            heightPx = (pageHeightPt * scale).roundToInt().coerceAtLeast(1)
        )
    }
}
