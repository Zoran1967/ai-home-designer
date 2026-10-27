package com.zoran.aihomedesigner.domain

import java.math.BigDecimal

/** Koja mera sobe se menja dodirom na planu. */
enum class MeasureKind { LENGTH, WIDTH, HEIGHT }

/** Mesto na planu (u metrima) gde se prikazuje mera sobe koju korisnik može da dodirne. */
data class MeasureAnchor(val kind: MeasureKind, val xM: Double, val yM: Double)

/** Izmena jedne mere sobe (dužina, širina ili visina zida) dodirom na plan. Čista logika, bez Androida. */
object RoomMeasureEdit {

    /**
     * Mesta mera za sobu: dužina na gornjoj ivici, širina na levoj ivici, visina na donjoj ivici (sredine ivica).
     * Soba bez položaja na planu nema mesta za prikaz.
     */
    fun anchors(room: Room): List<MeasureAnchor> {
        val x = room.xM ?: return emptyList()
        val y = room.yM ?: return emptyList()
        return listOf(
            MeasureAnchor(MeasureKind.LENGTH, x + room.lengthM / 2, y),
            MeasureAnchor(MeasureKind.WIDTH, x, y + room.widthM / 2),
            MeasureAnchor(MeasureKind.HEIGHT, x + room.lengthM / 2, y + room.widthM)
        )
    }

    fun valueOf(room: Room, kind: MeasureKind): Double = when (kind) {
        MeasureKind.LENGTH -> room.lengthM
        MeasureKind.WIDTH -> room.widthM
        MeasureKind.HEIGHT -> room.heightM
    }

    /** Najmanja i najveća dozvoljena vrednost mere (ista pravila kao u formi za sobu). */
    fun limits(kind: MeasureKind): Pair<Double, Double> = when (kind) {
        MeasureKind.LENGTH, MeasureKind.WIDTH -> DimensionLimits.MIN_LENGTH_M to DimensionLimits.MAX_LENGTH_M
        MeasureKind.HEIGHT -> DimensionLimits.MIN_HEIGHT_M to DimensionLimits.MAX_HEIGHT_M
    }

    /**
     * Unos za izmenu sobe u kojem je samo mera [kind] zamenjena tekstom [newText], a sve ostalo ostaje kako je
     * u sobi (ime, ostale mere, debljina zida). Tako se menja samo ono što je korisnik dodirnuo.
     */
    fun input(room: Room, kind: MeasureKind, newText: String): RoomInput = RoomInput(
        name = room.name,
        length = if (kind == MeasureKind.LENGTH) newText else inputText(room.lengthM),
        width = if (kind == MeasureKind.WIDTH) newText else inputText(room.widthM),
        height = if (kind == MeasureKind.HEIGHT) newText else inputText(room.heightM),
        wallThickness = room.wallThicknessM?.let { inputText(it) } ?: ""
    )

    /** Mera kao tekst za polje za unos: decimalni zarez, bez suvišnih nula. Tekst se vraća u istu vrednost. */
    fun inputText(meters: Double): String =
        BigDecimal(meters.toString()).stripTrailingZeros().toPlainString().replace('.', ',')
}
