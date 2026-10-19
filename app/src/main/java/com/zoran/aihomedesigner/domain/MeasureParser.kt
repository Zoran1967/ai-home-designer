package com.zoran.aihomedesigner.domain

sealed interface MeasureParse {
    data class Value(val meters: Double) : MeasureParse
    data class Failure(val error: DimensionError) : MeasureParse
}

/** Pretvara tekst koji je korisnik upisao u metre. Prihvata zarez ili tačku kao decimalni znak. */
object MeasureParser {

    private val numberPattern = Regex("^\\d+([.,]\\d+)?$")

    fun parse(raw: String, min: Double, max: Double): MeasureParse {
        val text = raw.trim()
        if (text.isEmpty()) return MeasureParse.Failure(DimensionError.MISSING)
        if (!numberPattern.matches(text)) return MeasureParse.Failure(DimensionError.NOT_A_NUMBER)
        val value = text.replace(',', '.').toDouble()
        if (!value.isFinite() || value > max) return MeasureParse.Failure(DimensionError.TOO_LARGE)
        if (value < min) return MeasureParse.Failure(DimensionError.TOO_SMALL)
        return MeasureParse.Value(value)
    }
}
