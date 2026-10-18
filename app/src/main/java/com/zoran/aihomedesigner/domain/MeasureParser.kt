package com.zoran.aihomedesigner.domain

sealed interface MeasureResult {
    data class Value(val meters: Double) : MeasureResult
    data class Failure(val error: DimensionError) : MeasureResult
}

/** Čitanje mere u metrima iz teksta koji je korisnik upisao. Prihvata zarez ili tačku ("3,5" i "3.5"). */
object MeasureParser {

    private val numberPattern = Regex("^\\d+([.,]\\d+)?$")

    fun parse(raw: String, min: Double, max: Double): MeasureResult {
        val text = raw.trim()
        if (text.isEmpty()) return MeasureResult.Failure(DimensionError.MISSING)
        if (!numberPattern.matches(text)) return MeasureResult.Failure(DimensionError.NOT_A_NUMBER)
        val value = text.replace(',', '.').toDouble()
        if (!value.isFinite() || value > max) return MeasureResult.Failure(DimensionError.TOO_LARGE)
        if (value < min) return MeasureResult.Failure(DimensionError.TOO_SMALL)
        return MeasureResult.Value(value)
    }
}
