package com.zoran.aihomedesigner.ui

import java.math.BigDecimal
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

/** Metri pretvoreni u centimetre za prikaz, do jedne decimale (npr. 0,2 m -> 20). Samo prikaz. */
fun formatCentimeters(valueM: Double): String = format(valueM * 100.0, "0.#")

/**
 * Tekst za polje za izmenu: tačna vrednost bez zaokruživanja, sa zarezom (npr. 4,5 ili 3,4567).
 * Zaokruživanje ovde bi tiho promenilo sačuvanu meru pri ponovnom čuvanju.
 */
fun metersToInputText(value: Double): String =
    BigDecimal(value.toString()).stripTrailingZeros().toPlainString().replace('.', ',')
