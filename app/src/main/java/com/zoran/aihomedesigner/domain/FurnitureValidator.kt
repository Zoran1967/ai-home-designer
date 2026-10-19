package com.zoran.aihomedesigner.domain

/** Granice služe samo da uhvate očigledne greške u unosu. Ne garantuju tačnost mere. */
object FurnitureLimits {
    const val MIN_SIDE_M = 0.1
    const val MAX_SIDE_M = 5.0
    const val MIN_HEIGHT_M = 0.1
    const val MAX_HEIGHT_M = 3.0
    const val MAX_NAME_LENGTH = 40
}

enum class FurnitureNameError { EMPTY, TOO_LONG }

/** Tekst koji je korisnik upisao, tačno kako ga je upisao, plus izabrana vrsta. */
data class FurnitureInput(
    val name: String,
    val category: FurnitureCategory?,
    val length: String,
    val width: String,
    val height: String
)

data class FurnitureInputErrors(
    val name: FurnitureNameError?,
    val categoryMissing: Boolean,
    val length: DimensionError?,
    val width: DimensionError?,
    val height: DimensionError?,
    /** Predmet ne staje u sobu ni u jednom položaju (ni kad se okrene za 90 stepeni). */
    val tooBigForRoom: Boolean
) {
    val hasErrors: Boolean
        get() = name != null || categoryMissing || length != null || width != null ||
            height != null || tooBigForRoom
}

sealed interface FurnitureValidation {
    data class Valid(
        val name: String,
        val category: FurnitureCategory,
        val lengthM: Double,
        val widthM: Double,
        val heightM: Double
    ) : FurnitureValidation

    data class Invalid(val errors: FurnitureInputErrors) : FurnitureValidation
}

object FurnitureValidator {

    private val numberPattern = Regex("^\\d+([.,]\\d+)?$")

    private sealed interface Parsed {
        data class Value(val meters: Double) : Parsed
        data class Failure(val error: DimensionError) : Parsed
    }

    private fun parse(raw: String, min: Double, max: Double): Parsed {
        val text = raw.trim()
        if (text.isEmpty()) return Parsed.Failure(DimensionError.MISSING)
        if (!numberPattern.matches(text)) return Parsed.Failure(DimensionError.NOT_A_NUMBER)
        val value = text.replace(',', '.').toDouble()
        if (!value.isFinite() || value > max) return Parsed.Failure(DimensionError.TOO_LARGE)
        if (value < min) return Parsed.Failure(DimensionError.TOO_SMALL)
        return Parsed.Value(value)
    }

    fun validate(input: FurnitureInput, room: Room): FurnitureValidation {
        val name = ProjectValidator.normalizeName(input.name)
        val nameError = when {
            name.isEmpty() -> FurnitureNameError.EMPTY
            name.length > FurnitureLimits.MAX_NAME_LENGTH -> FurnitureNameError.TOO_LONG
            else -> null
        }

        val length = parse(input.length, FurnitureLimits.MIN_SIDE_M, FurnitureLimits.MAX_SIDE_M)
        val width = parse(input.width, FurnitureLimits.MIN_SIDE_M, FurnitureLimits.MAX_SIDE_M)
        val height = parse(input.height, FurnitureLimits.MIN_HEIGHT_M, FurnitureLimits.MAX_HEIGHT_M)

        var tooBig = false
        if (length is Parsed.Value && width is Parsed.Value) {
            val eps = RoomGeometry.EPSILON_M
            val fitsAsIs = length.meters <= room.lengthM + eps && width.meters <= room.widthM + eps
            val fitsTurned = width.meters <= room.lengthM + eps && length.meters <= room.widthM + eps
            tooBig = !fitsAsIs && !fitsTurned
        }

        val errors = FurnitureInputErrors(
            name = nameError,
            categoryMissing = input.category == null,
            length = (length as? Parsed.Failure)?.error,
            width = (width as? Parsed.Failure)?.error,
            height = (height as? Parsed.Failure)?.error,
            tooBigForRoom = tooBig
        )
        if (errors.hasErrors) return FurnitureValidation.Invalid(errors)

        return FurnitureValidation.Valid(
            name = name,
            category = input.category!!,
            lengthM = (length as Parsed.Value).meters,
            widthM = (width as Parsed.Value).meters,
            heightM = (height as Parsed.Value).meters
        )
    }
}
