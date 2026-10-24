package com.zoran.aihomedesigner.domain

/** Tekst koji je korisnik upisao, tačno kako ga je upisao, plus izbori iz ponuđenih opcija. */
data class StairsInput(
    val lowerLevel: Int,
    val length: String,
    val width: String,
    val directionDeg: Int
)

data class StairsInputErrors(
    /** Donji sprat ili sprat iznad njega nije u dozvoljenim granicama ([Floors.MIN_LEVEL] do [Floors.MAX_LEVEL]). */
    val levelInvalid: Boolean,
    val length: DimensionError?,
    val width: DimensionError?,
    val directionInvalid: Boolean
) {
    val hasErrors: Boolean
        get() = levelInvalid || length != null || width != null || directionInvalid
}

sealed interface StairsValidation {
    data class Valid(
        val lowerLevel: Int,
        val lengthM: Double,
        val widthM: Double,
        val directionDeg: Int
    ) : StairsValidation

    data class Invalid(val errors: StairsInputErrors) : StairsValidation
}

/** Provera unosa za stepenice (sprat, mere, pravac). Udobnost uspona i gazišta proverava [StairsGeometry]. */
object StairsValidator {

    fun validate(input: StairsInput): StairsValidation {
        val levelOk = Floors.isValidLevel(input.lowerLevel) && Floors.isValidLevel(input.lowerLevel + 1)
        val length = MeasureParser.parse(input.length, StairsLimits.MIN_LENGTH_M, StairsLimits.MAX_LENGTH_M)
        val width = MeasureParser.parse(input.width, StairsLimits.MIN_WIDTH_M, StairsLimits.MAX_WIDTH_M)
        val direction = FurnitureValidator.normalizeRotation(input.directionDeg)

        val errors = StairsInputErrors(
            levelInvalid = !levelOk,
            length = (length as? MeasureParse.Failure)?.error,
            width = (width as? MeasureParse.Failure)?.error,
            directionInvalid = direction == null
        )
        if (errors.hasErrors) return StairsValidation.Invalid(errors)

        return StairsValidation.Valid(
            lowerLevel = input.lowerLevel,
            lengthM = (length as MeasureParse.Value).meters,
            widthM = (width as MeasureParse.Value).meters,
            directionDeg = direction!!
        )
    }
}
