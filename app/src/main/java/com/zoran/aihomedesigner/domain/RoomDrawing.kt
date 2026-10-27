package com.zoran.aihomedesigner.domain

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Pravougaonik koji je korisnik nacrtao preko slike plana. ([xM], [yM]) je gornji levi ugao u koordinatama plana,
 * [lengthM] je mera duž x ose (istok-zapad), a [widthM] duž y ose (sever-jug), sve u metrima.
 */
data class DrawnRect(val xM: Double, val yM: Double, val lengthM: Double, val widthM: Double)

enum class DrawAxis { LENGTH, WIDTH }

/** Upisana mera se razlikuje od mere koju pokazuje slika za više od [RoomDrawing.TOLERANCE]. */
data class RoomDrawMismatch(val axis: DrawAxis, val drawnM: Double, val typedM: Double)

/** Logika crtanja sobe preko slike plana (čista logika, bez Androida). */
object RoomDrawing {

    /** Najmanja strana koja se prihvata kao soba. Kraće prevlačenje je verovatno slučajno. */
    const val MIN_SIDE_M = DimensionLimits.MIN_LENGTH_M

    /** Dozvoljeno odstupanje upisane mere od mere sa slike (10%) pre nego što se prikaže upozorenje. */
    const val TOLERANCE = 0.10

    private const val EPS = 1e-9

    /**
     * Pravougaonik između dve tačke prevlačenja ([a] i [b] mogu biti bilo koji suprotni uglovi).
     * Uglovi se zaokružuju na korak plana (5 cm), kao pri pomeranju soba.
     * Vraća null ako je neka strana kraća od [MIN_SIDE_M].
     */
    fun rect(a: PlanPoint, b: PlanPoint): DrawnRect? {
        val x0 = RoomPlacement.clampCoordinate(RoomPlacement.snapToGrid(min(a.xM, b.xM)))
        val y0 = RoomPlacement.clampCoordinate(RoomPlacement.snapToGrid(min(a.yM, b.yM)))
        val x1 = RoomPlacement.clampCoordinate(RoomPlacement.snapToGrid(max(a.xM, b.xM)))
        val y1 = RoomPlacement.clampCoordinate(RoomPlacement.snapToGrid(max(a.yM, b.yM)))
        val length = RoomPlacement.roundToMm(x1 - x0)
        val width = RoomPlacement.roundToMm(y1 - y0)
        if (length < MIN_SIDE_M - EPS || width < MIN_SIDE_M - EPS) return null
        return DrawnRect(x0, y0, length, width)
    }

    /**
     * Poredi mere koje je korisnik upisao ([typedLengthM], [typedWidthM], null ako još nisu ispravno upisane)
     * sa merama sa slike. Upisana mera je uvek mera sobe. Ovo samo upozorava na moguću grešku u kucanju
     * ili lošu kalibraciju slike, a ništa ne menja.
     */
    fun mismatches(rect: DrawnRect, typedLengthM: Double?, typedWidthM: Double?): List<RoomDrawMismatch> =
        buildList {
            if (typedLengthM != null && differs(rect.lengthM, typedLengthM)) {
                add(RoomDrawMismatch(DrawAxis.LENGTH, rect.lengthM, typedLengthM))
            }
            if (typedWidthM != null && differs(rect.widthM, typedWidthM)) {
                add(RoomDrawMismatch(DrawAxis.WIDTH, rect.widthM, typedWidthM))
            }
        }

    private fun differs(drawnM: Double, typedM: Double): Boolean =
        abs(typedM - drawnM) / drawnM > TOLERANCE + EPS
}
