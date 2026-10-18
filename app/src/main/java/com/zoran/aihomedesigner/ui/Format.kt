package com.zoran.aihomedesigner.ui

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private val serbian: Locale = Locale.forLanguageTag("sr-RS")

private fun format(value: Double, pattern: String): String =
    DecimalFormat(pattern, DecimalFormatSymbols(serbian)).format(value)

/** Metri za prikaz, do milimetra, sa zarezom (npr. 3,456). Samo prikaz, vrednost se ne menja. */
fun formatMeters(value: Double): String = format(value, "0.###")

/** Površina za prikaz, do dve decimale (npr. 14,4). */
fun formatArea(value: Double): String = format(value, "0.##")
