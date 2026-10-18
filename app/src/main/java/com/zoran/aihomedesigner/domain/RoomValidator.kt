package com.zoran.aihomedesigner.domain

enum class RoomNameError { EMPTY, TOO_LONG, DUPLICATE }

enum class DimensionError { MISSING, NOT_A_NUMBER, TOO_SMALL, TOO_LARGE }

/**
 * Granice služe samo da uhvate očigledne greške u unosu (npr. 350 umesto 3,5).
 * Ne garantuju tačnost mere.
 */
object DimensionLimits {
    const val MIN_LENGTH_M = 0.5
    const val MAX_LENGTH_M = 50.0
    const val MIN_HEIGHT_M = 1.8
    const val MAX_HEIGHT_M = 6.0
    const val MIN_THICKNESS_M = 0.05
    const val MAX_THICKNESS_M = 1.0
}

/** Tekst koji je korisnik upisao, tačno kako ga je upisao. */
data class RoomInput(
    val name: String,
    val length: String,
    val width: String,
    val height: String,
    val wallThickness: String
)

data class RoomInputErrors(
    val name: RoomNameError?,
    val length: DimensionError?,
    val width: DimensionError?,
    val height: DimensionError?,
    val wallThickness: DimensionError?
) {
    val hasErrors: Boolean
        get() = name != null || length != null || width != null ||
            height != null || wallThickness != null
}

sealed interface RoomValidation {
    data class Valid(
        val name: String,
        val lengthM: Double,
        val widthM: Double,
        val heightM: Double,
        val wallThicknessM: Double?
    ) : RoomValidation

    data class Invalid(val errors: RoomInputErrors) : RoomValidation
}

object RoomValidator {

    const val MAX_NAME_LENGTH = 40

    private val numberPattern = Regex("^\\d+([.,]\\d+)?$")

    private sealed interface Parsed {
        data class Value(val meters: Double) : Parsed
        data class Failure(val error: DimensionError) : Parsed
    }

    /** Prihvata zarez ili tačku kao decimalni znak ("3,5" i "3.5"). Mere su u metrima. */
    private fun parse(raw: String, min: Double, max: Double): Parsed {
        val text = raw.trim()
        if (text.isEmpty()) return Parsed.Failure(DimensionError.MISSING)
        if (!numberPattern.matches(text)) return Parsed.Failure(DimensionError.NOT_A_NUMBER)
        val value = text.replace(',', '.').toDouble()
        if (!value.isFinite() || value > max) return Parsed.Failure(DimensionError.TOO_LARGE)
        if (value < min) return Parsed.Failure(DimensionError.TOO_SMALL)
        return Parsed.Value(value)
    }

    /** Proverava sva polja odjednom, da korisnik vidi sve greške. */
    fun validate(input: RoomInput, existingNames: List<String>): RoomValidation {
        val name = ProjectValidator.normalizeName(input.name)
        val nameError = when {
            name.isEmpty() -> RoomNameError.EMPTY
            name.length > MAX_NAME_LENGTH -> RoomNameError.TOO_LONG
            existingNames.any {
                ProjectValidator.normalizeName(it).equals(name, ignoreCase = true)
            } -> RoomNameError.DUPLICATE
            else -> null
        }

        val length = parse(input.length, DimensionLimits.MIN_LENGTH_M, DimensionLimits.MAX_LENGTH_M)
        val width = parse(input.width, DimensionLimits.MIN_LENGTH_M, DimensionLimits.MAX_LENGTH_M)
        val height = parse(input.height, DimensionLimits.MIN_HEIGHT_M, DimensionLimits.MAX_HEIGHT_M)
        val thickness: Parsed? =
            if (input.wallThickness.isBlank()) null
            else parse(input.wallThickness, DimensionLimits.MIN_THICKNESS_M, DimensionLimits.MAX_THICKNESS_M)

        val errors = RoomInputErrors(
            name = nameError,
            length = (length as? Parsed.Failure)?.error,
            width = (width as? Parsed.Failure)?.error,
            height = (height as? Parsed.Failure)?.error,
            wallThickness = (thickness as? Parsed.Failure)?.error
        )
        if (errors.hasErrors) return RoomValidation.Invalid(errors)

        return RoomValidation.Valid(
            name = name,
            lengthM = (length as Parsed.Value).meters,
            widthM = (width as Parsed.Value).meters,
            heightM = (height as Parsed.Value).meters,
            wallThicknessM = (thickness as? Parsed.Value)?.meters
        )
    }
}
