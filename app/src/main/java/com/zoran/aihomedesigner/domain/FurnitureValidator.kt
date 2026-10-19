package com.zoran.aihomedesigner.domain

enum class FurnitureNameError { EMPTY, TOO_LONG }

/** Granice služe samo da uhvate očigledne greške u unosu. Ne garantuju tačnost mere. */
object FurnitureLimits {
    const val MAX_NAME_LENGTH = 40
    const val MIN_SIZE_M = 0.1
    const val MAX_SIZE_M = 6.0
    const val MIN_HEIGHT_M = 0.05
    const val MAX_HEIGHT_M = 3.5
}

/** Tekst koji je korisnik upisao, tačno kako ga je upisao, plus izbori iz ponuđenih opcija. */
data class FurnitureInput(
    val name: String,
    val category: FurnitureCategory,
    val length: String,
    val width: String,
    val height: String,
    val rotationDeg: Int
)

data class FurnitureInputErrors(
    val name: FurnitureNameError?,
    val length: DimensionError?,
    val width: DimensionError?,
    val height: DimensionError?,
    val rotationInvalid: Boolean
) {
    val hasErrors: Boolean
        get() = name != null || length != null || width != null || height != null || rotationInvalid
}

sealed interface FurnitureValidation {
    data class Valid(
        val name: String,
        val lengthM: Double,
        val widthM: Double,
        val heightM: Double,
        val rotationDeg: Int
    ) : FurnitureValidation

    data class Invalid(val errors: FurnitureInputErrors) : FurnitureValidation
}

/** Provera unosa za nameštaj (ime, mere, okretanje). Raspored u sobi proverava FurnitureLayoutValidator. */
object FurnitureValidator {

    /** Svodi ugao na 0, 90, 180 ili 270. Vraća null ako ugao nije umnožak od 90. */
    fun normalizeRotation(degrees: Int): Int? {
        if (degrees % 90 != 0) return null
        return ((degrees % 360) + 360) % 360
    }

    fun validate(input: FurnitureInput): FurnitureValidation {
        val name = ProjectValidator.normalizeName(input.name)
        val nameError = when {
            name.isEmpty() -> FurnitureNameError.EMPTY
            name.length > FurnitureLimits.MAX_NAME_LENGTH -> FurnitureNameError.TOO_LONG
            else -> null
        }

        val length = MeasureParser.parse(input.length, FurnitureLimits.MIN_SIZE_M, FurnitureLimits.MAX_SIZE_M)
        val width = MeasureParser.parse(input.width, FurnitureLimits.MIN_SIZE_M, FurnitureLimits.MAX_SIZE_M)
        val height = MeasureParser.parse(input.height, FurnitureLimits.MIN_HEIGHT_M, FurnitureLimits.MAX_HEIGHT_M)
        val rotation = normalizeRotation(input.rotationDeg)

        val errors = FurnitureInputErrors(
            name = nameError,
            length = (length as? MeasureParse.Failure)?.error,
            width = (width as? MeasureParse.Failure)?.error,
            height = (height as? MeasureParse.Failure)?.error,
            rotationInvalid = rotation == null
        )
        if (errors.hasErrors) return FurnitureValidation.Invalid(errors)

        return FurnitureValidation.Valid(
            name = name,
            lengthM = (length as MeasureParse.Value).meters,
            widthM = (width as MeasureParse.Value).meters,
            heightM = (height as MeasureParse.Value).meters,
            rotationDeg = rotation!!
        )
    }
}
