package com.zoran.aihomedesigner.domain

import kotlin.math.max

/**
 * Podešavanje razmere uvezene slike plana prema meri koju je korisnik upisao za nacrtanu sobu.
 * Cela slika se skalira oko jedne tačke plana (gornji levi ugao nacrtane sobe), pa ta tačka ostaje na mestu.
 * Čista logika, bez Androida.
 */
object ImageRescale {

    /** Najmanji i najveći dozvoljeni faktor. Veće odstupanje je verovatnije greška u kucanju nego loša razmera. */
    const val MIN_FACTOR = 0.5
    const val MAX_FACTOR = 2.0

    /**
     * Faktor za koji treba pomnožiti razmeru slike da bi mera [drawnM] sa slike postala [typedM].
     * Vraća null ako je faktor van [MIN_FACTOR]..[MAX_FACTOR] ili mere nisu veće od nule.
     */
    fun factor(drawnM: Double, typedM: Double): Double? {
        if (!(drawnM > 0.0) || !(typedM > 0.0)) return null
        val k = typedM / drawnM
        return k.takeIf { it >= MIN_FACTOR && it <= MAX_FACTOR }
    }

    /**
     * Slika sa razmerom pomnoženom faktorom [factor], skalirana oko tačke ([anchorXM], [anchorYM]) plana.
     * Izvor ostaje IMPORTED_CALIBRATED. Vraća null ako bi duža strana slike dobila nerealnu veličinu
     * ([Calibration.MIN_IMAGE_SIDE_M] do [Calibration.MAX_IMAGE_SIDE_M]) ili neispravan položaj.
     */
    fun apply(image: PlanImage, factor: Double, anchorXM: Double, anchorYM: Double): PlanImage? {
        if (!(factor > 0.0)) return null
        val metersPerPixel = image.metersPerPixel * factor
        val longSideM = max(image.widthPx, image.heightPx) * metersPerPixel
        if (longSideM < Calibration.MIN_IMAGE_SIDE_M || longSideM > Calibration.MAX_IMAGE_SIDE_M) return null
        val originX = anchorXM + (image.originXM - anchorXM) * factor
        val originY = anchorYM + (image.originYM - anchorYM) * factor
        if (!RoomPlacement.isValidCoordinate(originX) || !RoomPlacement.isValidCoordinate(originY)) return null
        return image.copy(
            metersPerPixel = metersPerPixel,
            originXM = originX,
            originYM = originY,
            dimensionSource = DimensionSource.IMPORTED_CALIBRATED
        )
    }

    /** Nacrtani pravougaonik posle skaliranja slike: gornji levi ugao ostaje, a strane se množe faktorom. */
    fun scaleRect(rect: DrawnRect, factor: Double): DrawnRect =
        DrawnRect(rect.xM, rect.yM, rect.lengthM * factor, rect.widthM * factor)
}
