package com.zoran.aihomedesigner.domain

import kotlin.math.max
import kotlin.math.roundToInt

/** Pravougaonik na planu, u metrima: (x0, y0) je gornji levi ugao, (x1, y1) donji desni. */
data class StairsFootprint(val x0: Double, val y0: Double, val x1: Double, val y1: Double)

enum class StairsAdvice {
    /** Stepenik je viši od preporučenog. */
    RISER_TOO_HIGH,

    /** Gazište (dubina stepenika) je kraće od preporučenog. */
    TREAD_TOO_SHORT
}

/**
 * Procena stepenica za zadatu visinu i dužinu kraka.
 * [stepCount] je broj stepenika (uspona), a gazišta ima jedno manje: poslednji stepenik je sam sprat.
 */
data class StairsComfort(
    val stepCount: Int,
    val riserM: Double,
    val treadM: Double,
    val advice: List<StairsAdvice>
)

object StairsGeometry {

    /** Ciljna visina stepenika pri računanju broja stepenika. */
    const val TARGET_RISER_M = 0.18

    /**
     * Preporuke za udobnost. To su uobičajene orijentacione vrednosti, a ne provera po propisima.
     * Odstupanje daje savet, ne grešku.
     */
    const val MAX_COMFORT_RISER_M = 0.19
    const val MIN_COMFORT_TREAD_M = 0.25

    /** Površina koju stepenice zauzimaju na planu. */
    fun footprint(stairs: Stairs): StairsFootprint = StairsFootprint(
        x0 = stairs.xM,
        y0 = stairs.yM,
        x1 = stairs.xM + stairs.footprintLengthM,
        y1 = stairs.yM + stairs.footprintWidthM
    )

    /** Broj stepenika za visinu [riseM] (od poda sprata do poda sprata iznad), najmanje 2. */
    fun stepCount(riseM: Double): Int {
        require(riseM.isFinite() && riseM > 0) { "Visina mora biti veća od nule" }
        return max(2, (riseM / TARGET_RISER_M).roundToInt())
    }

    /** Računa visinu stepenika i dubinu gazišta za visinu [riseM] i dužinu kraka [runM], pa daje savete. */
    fun analyze(riseM: Double, runM: Double): StairsComfort {
        require(runM.isFinite() && runM > 0) { "Dužina kraka mora biti veća od nule" }
        val steps = stepCount(riseM)
        val riser = riseM / steps
        val tread = runM / (steps - 1)
        val advice = buildList {
            if (riser > MAX_COMFORT_RISER_M) add(StairsAdvice.RISER_TOO_HIGH)
            if (tread < MIN_COMFORT_TREAD_M) add(StairsAdvice.TREAD_TOO_SHORT)
        }
        return StairsComfort(steps, riser, tread, advice)
    }
}
