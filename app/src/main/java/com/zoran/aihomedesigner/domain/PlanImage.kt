package com.zoran.aihomedesigner.domain

import kotlin.math.hypot
import kotlin.math.max

/**
 * Uvezena slika plana (podloga) jednog projekta, sa razmerom koju je korisnik zadao.
 *
 * Slika se kopira u privatni prostor aplikacije, a fileName je ime te kopije.
 * metersPerPixel je koliko metara predstavlja jedan piksel slike. Dobija se kalibracijom: korisnik
 * obeleži dve tačke na slici i upiše pravo rastojanje među njima, pa je tačnost razmere onoliko dobra
 * koliko je tačno obeležena mera na slici. Zato se izvor vodi kao IMPORTED_CALIBRATED, a ne kao izmereno.
 * (originXM, originYM) je položaj gornjeg levog ugla slike u koordinatama plana, u metrima.
 */
data class PlanImage(
    val projectId: String,
    val fileName: String,
    val widthPx: Int,
    val heightPx: Int,
    val metersPerPixel: Double,
    val originXM: Double,
    val originYM: Double,
    val dimensionSource: DimensionSource,
    val createdAtMillis: Long
) {
    val widthM: Double get() = widthPx * metersPerPixel
    val heightM: Double get() = heightPx * metersPerPixel
}

sealed interface CalibrationResult {
    data class Ok(val metersPerPixel: Double) : CalibrationResult

    data class Invalid(
        /** Dve obeležene tačke su preblizu jedna drugoj da bi se odredila razmera. */
        val pointsTooClose: Boolean,
        /** Problem sa upisanim pravim rastojanjem (null ako je ispravno). */
        val distanceError: DimensionError?,
        /** Dobijena razmera daje nerealno veliku ili malu sliku (verovatno greška u unosu). */
        val implausibleScale: Boolean
    ) : CalibrationResult
}

object Calibration {

    /** Najmanje rastojanje između dve obeležene tačke na slici, u pikselima. */
    const val MIN_POINT_DISTANCE_PX = 20.0

    const val MIN_KNOWN_DISTANCE_M = 0.1
    const val MAX_KNOWN_DISTANCE_M = 100.0

    /** Duža strana slike mora da predstavlja između 1 m i 500 m, inače je razmera verovatno pogrešna. */
    const val MIN_IMAGE_SIDE_M = 1.0
    const val MAX_IMAGE_SIDE_M = 500.0

    /**
     * Računa razmeru iz dve tačke na slici (u pikselima) i pravog rastojanja među njima (tekst koji je
     * korisnik upisao, u metrima, sa zarezom ili tačkom).
     */
    fun calibrate(
        ax: Double,
        ay: Double,
        bx: Double,
        by: Double,
        distanceText: String,
        imageWidthPx: Int,
        imageHeightPx: Int
    ): CalibrationResult {
        val pixelDistance = hypot(bx - ax, by - ay)
        val tooClose = !(pixelDistance >= MIN_POINT_DISTANCE_PX)

        val parsed = MeasureParser.parse(distanceText, MIN_KNOWN_DISTANCE_M, MAX_KNOWN_DISTANCE_M)
        val distanceError = (parsed as? MeasureParse.Failure)?.error

        if (tooClose || distanceError != null) {
            return CalibrationResult.Invalid(tooClose, distanceError, implausibleScale = false)
        }

        val metersPerPixel = (parsed as MeasureParse.Value).meters / pixelDistance
        val longSideM = max(imageWidthPx, imageHeightPx) * metersPerPixel
        if (longSideM < MIN_IMAGE_SIDE_M || longSideM > MAX_IMAGE_SIDE_M) {
            return CalibrationResult.Invalid(pointsTooClose = false, distanceError = null, implausibleScale = true)
        }
        return CalibrationResult.Ok(metersPerPixel)
    }
}
