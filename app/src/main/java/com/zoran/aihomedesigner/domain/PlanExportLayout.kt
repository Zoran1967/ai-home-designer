package com.zoran.aihomedesigner.domain

import kotlin.math.max
import kotlin.math.min

/** Kako se plan preslikava na stranicu: razmera (piksela po metru) i pomeraj, u pikselima stranice. */
data class ExportFrame(val scale: Double, val offsetX: Double, val offsetY: Double) {
    fun x(planX: Double): Double = offsetX + planX * scale
    fun y(planY: Double): Double = offsetY + planY * scale
}

/** Računanje razmeštaja plana na slici ili PDF stranici pri izvozu. Bez Androida, pa se lako testira. */
object PlanExportLayout {

    /** Dozvoljene dužine lenjira razmere, u metrima. */
    private val SCALE_BAR_CHOICES = doubleArrayOf(0.1, 0.2, 0.5, 1.0, 2.0, 5.0, 10.0, 20.0, 50.0, 100.0)

    /**
     * Pravougaonik koji obuhvata sve sobe sa položajem na planu, zajedno sa zidovima (onako kako se
     * crtaju). Vraća null ako nijedna soba nema položaj.
     */
    fun bounds(rooms: List<Room>): PlanRect? {
        val placed = rooms.filter { it.isPlaced }
        if (placed.isEmpty()) return null
        var x0 = Double.MAX_VALUE
        var y0 = Double.MAX_VALUE
        var x1 = -Double.MAX_VALUE
        var y1 = -Double.MAX_VALUE
        for (room in placed) {
            val t = SceneBuilder.wallThicknessForDisplay(room)
            x0 = min(x0, room.xM!! - t)
            y0 = min(y0, room.yM!! - t)
            x1 = max(x1, room.xM + room.lengthM + t)
            y1 = max(y1, room.yM!! + room.widthM + t)
        }
        return PlanRect(x0, y0, x1, y1)
    }

    /**
     * Uklapa [bounds] u stranicu [pageWidth] x [pageHeight] sa marginom, uz rezervisan prostor za naslov
     * gore i za legendu dole. Plan se centrira u preostalom prostoru.
     */
    fun frame(
        bounds: PlanRect,
        pageWidth: Double,
        pageHeight: Double,
        margin: Double,
        topReserved: Double,
        bottomReserved: Double
    ): ExportFrame {
        val availableWidth = max(pageWidth - 2 * margin, 1.0)
        val availableHeight = max(pageHeight - 2 * margin - topReserved - bottomReserved, 1.0)
        val planWidth = max(bounds.x1 - bounds.x0, 0.1)
        val planHeight = max(bounds.y1 - bounds.y0, 0.1)
        val scale = min(availableWidth / planWidth, availableHeight / planHeight)

        val offsetX = margin + (availableWidth - planWidth * scale) / 2.0 - bounds.x0 * scale
        val offsetY = margin + topReserved + (availableHeight - planHeight * scale) / 2.0 - bounds.y0 * scale
        return ExportFrame(scale, offsetX, offsetY)
    }

    /**
     * Dužina lenjira razmere u metrima: najveća ponuđena vrednost koja na stranici zauzima najviše
     * [maxLengthPx] piksela. Ako ni najmanja ne staje, vraća se najmanja.
     */
    fun scaleBarMeters(scale: Double, maxLengthPx: Double): Double {
        require(scale > 0.0) { "scale mora biti veći od nule" }
        return SCALE_BAR_CHOICES.lastOrNull { it * scale <= maxLengthPx } ?: SCALE_BAR_CHOICES.first()
    }
}
