package com.zoran.aihomedesigner.domain

/**
 * Granice služe samo da uhvate očigledne greške u unosu. Ne garantuju tačnost mere.
 */
object OpeningLimits {
    const val OFFSET_MAX_M = 50.0
    const val DOOR_MIN_WIDTH_M = 0.5
    const val DOOR_MAX_WIDTH_M = 3.0
    const val DOOR_MIN_HEIGHT_M = 1.6
    const val DOOR_MAX_HEIGHT_M = 3.0
    const val WINDOW_MIN_WIDTH_M = 0.3
    const val WINDOW_MAX_WIDTH_M = 5.0
    const val WINDOW_MIN_HEIGHT_M = 0.3
    const val WINDOW_MAX_HEIGHT_M = 3.0
    const val SILL_MAX_M = 2.5
}

/** Problem sa smeštajem otvora, koji se proverava tek kad su sve mere ispravno upisane. */
enum class OpeningPlacementError {
    /** Otvor ne staje u zid (izlazi preko kraja zida). */
    EXCEEDS_WALL,

    /** Otvor je viši od sobe (za prozor: donja ivica plus visina). */
    TOO_TALL_FOR_ROOM,

    /** Otvor se preklapa sa drugim otvorom na istom zidu. */
    OVERLAPS_OTHER
}

/** Tekst koji je korisnik upisao, tačno kako ga je upisao, plus izbori iz ponuđenih opcija. */
data class OpeningInput(
    val type: OpeningType,
    val side: WallSide,
    val offset: String,
    val width: String,
    val height: String,
    val sillHeight: String,
    val swing: DoorSwing?,
    val hinge: HingeEnd?
)

data class OpeningInputErrors(
    val offset: DimensionError?,
    val width: DimensionError?,
    val height: DimensionError?,
    val sillHeight: DimensionError?,
    val placement: OpeningPlacementError?,
    val swingMissing: Boolean,
    val hingeMissing: Boolean,
    /**
     * Kad otvor ne staje u zid: najveći položaj za koji bi uneta širina stala.
     * Negativna vrednost znači da je otvor širi od celog zida.
     */
    val maxOffsetM: Double? = null
) {
    val hasErrors: Boolean
        get() = offset != null || width != null || height != null || sillHeight != null ||
            placement != null || swingMissing || hingeMissing
}

sealed interface OpeningValidation {
    data class Valid(
        val offsetM: Double,
        val widthM: Double,
        val heightM: Double,
        val sillHeightM: Double,
        val swing: DoorSwing?,
        val hinge: HingeEnd?
    ) : OpeningValidation

    data class Invalid(val errors: OpeningInputErrors) : OpeningValidation
}

object OpeningValidator {

    private val numberPattern = Regex("^\\d+([.,]\\d+)?$")

    private sealed interface Parsed {
        data class Value(val meters: Double) : Parsed
        data class Failure(val error: DimensionError) : Parsed
    }

    /** Prihvata zarez ili tačku kao decimalni znak. Mere su u metrima. */
    private fun parse(raw: String, min: Double, max: Double): Parsed {
        val text = raw.trim()
        if (text.isEmpty()) return Parsed.Failure(DimensionError.MISSING)
        if (!numberPattern.matches(text)) return Parsed.Failure(DimensionError.NOT_A_NUMBER)
        val value = text.replace(',', '.').toDouble()
        if (!value.isFinite() || value > max) return Parsed.Failure(DimensionError.TOO_LARGE)
        if (value < min) return Parsed.Failure(DimensionError.TOO_SMALL)
        return Parsed.Value(value)
    }

    /** Vraća vrednost u metrima ako je tekst ispravan broj (zarez ili tačka), inače null. */
    fun parseMeters(raw: String): Double? {
        val text = raw.trim()
        if (!numberPattern.matches(text)) return null
        val value = text.replace(',', '.').toDouble()
        return if (value.isFinite()) value else null
    }

    /** Položaj otvora tako da bude na sredini zida, ili null ako je otvor širi od zida. */
    fun centeredOffsetM(wallLengthM: Double, widthM: Double): Double? {
        if (widthM > wallLengthM) return null
        return RoomPlacement.roundToMm((wallLengthM - widthM) / 2.0)
    }

    /**
     * Proverava unos za vrata ili prozor u zidu sobe [room].
     * [existingOnRoom] su drugi otvori u toj sobi. [editingId] je otvor koji se menja
     * (da se ne poredi sam sa sobom).
     */
    fun validate(
        input: OpeningInput,
        room: Room,
        existingOnRoom: List<Opening>,
        editingId: String? = null
    ): OpeningValidation {
        val isDoor = input.type == OpeningType.DOOR

        val minWidth = if (isDoor) OpeningLimits.DOOR_MIN_WIDTH_M else OpeningLimits.WINDOW_MIN_WIDTH_M
        val maxWidth = if (isDoor) OpeningLimits.DOOR_MAX_WIDTH_M else OpeningLimits.WINDOW_MAX_WIDTH_M
        val minHeight = if (isDoor) OpeningLimits.DOOR_MIN_HEIGHT_M else OpeningLimits.WINDOW_MIN_HEIGHT_M
        val maxHeight = if (isDoor) OpeningLimits.DOOR_MAX_HEIGHT_M else OpeningLimits.WINDOW_MAX_HEIGHT_M

        val offset = parse(input.offset, 0.0, OpeningLimits.OFFSET_MAX_M)
        val width = parse(input.width, minWidth, maxWidth)
        val height = parse(input.height, minHeight, maxHeight)
        // Donja ivica se unosi samo za prozor; vrata idu od poda.
        val sill: Parsed? = if (isDoor) null else parse(input.sillHeight, 0.0, OpeningLimits.SILL_MAX_M)

        val swingMissing = isDoor && input.swing == null
        val hingeMissing = isDoor && input.swing != null &&
            input.swing != DoorSwing.SLIDING && input.hinge == null

        var placement: OpeningPlacementError? = null
        var maxOffset: Double? = null
        val sillOk = isDoor || sill is Parsed.Value
        if (offset is Parsed.Value && width is Parsed.Value && height is Parsed.Value && sillOk) {
            val sillM = if (sill is Parsed.Value) sill.meters else 0.0
            val wallLength = WallGeometry.wallLengthM(room, input.side)
            val start = offset.meters
            val end = offset.meters + width.meters
            val eps = RoomGeometry.EPSILON_M

            placement = when {
                end > wallLength + eps -> OpeningPlacementError.EXCEEDS_WALL
                sillM + height.meters > room.heightM + eps -> OpeningPlacementError.TOO_TALL_FOR_ROOM
                existingOnRoom.any { other ->
                    other.id != editingId &&
                        other.side == input.side &&
                        start < other.offsetM + other.widthM - eps &&
                        other.offsetM < end - eps
                } -> OpeningPlacementError.OVERLAPS_OTHER

                else -> null
            }
            if (placement == OpeningPlacementError.EXCEEDS_WALL) {
                maxOffset = wallLength - width.meters
            }
        }

        val errors = OpeningInputErrors(
            offset = (offset as? Parsed.Failure)?.error,
            width = (width as? Parsed.Failure)?.error,
            height = (height as? Parsed.Failure)?.error,
            sillHeight = (sill as? Parsed.Failure)?.error,
            placement = placement,
            swingMissing = swingMissing,
            hingeMissing = hingeMissing,
            maxOffsetM = maxOffset
        )
        if (errors.hasErrors) return OpeningValidation.Invalid(errors)

        return OpeningValidation.Valid(
            offsetM = (offset as Parsed.Value).meters,
            widthM = (width as Parsed.Value).meters,
            heightM = (height as Parsed.Value).meters,
            sillHeightM = if (sill is Parsed.Value) sill.meters else 0.0,
            swing = if (isDoor) input.swing else null,
            hinge = if (isDoor && input.swing != DoorSwing.SLIDING) input.hinge else null
        )
    }
}
