package com.zoran.aihomedesigner.domain

enum class OpeningLayoutError {
    /** Otvor ne staje u zid (izlazi iza kraja zida). */
    OUT_OF_WALL,

    /** Otvor je viši od sobe (donja ivica plus visina prelazi visinu zida). */
    TOO_HIGH_FOR_ROOM,

    /** Otvor se preklapa sa drugim otvorom na istom zidu. */
    OVERLAPS_OTHER
}

/**
 * Granice služe samo da uhvate očigledne greške u unosu. Ne garantuju tačnost mere.
 */
object OpeningLimits {
    const val MAX_OFFSET_M = 50.0

    const val DOOR_MIN_WIDTH_M = 0.5
    const val DOOR_MAX_WIDTH_M = 3.0
    const val DOOR_MIN_HEIGHT_M = 1.8
    const val DOOR_MAX_HEIGHT_M = 3.0

    const val WINDOW_MIN_WIDTH_M = 0.3
    const val WINDOW_MAX_WIDTH_M = 5.0
    const val WINDOW_MIN_HEIGHT_M = 0.3
    const val WINDOW_MAX_HEIGHT_M = 3.0
    const val WINDOW_MIN_SILL_M = 0.0
    const val WINDOW_MAX_SILL_M = 2.5
}

/** Tekst koji je korisnik upisao, tačno kako ga je upisao, plus izbori sa liste. */
data class OpeningInput(
    val side: WallSide,
    val type: OpeningType,
    val offset: String,
    val width: String,
    val height: String,
    val sillHeight: String,
    val swing: DoorSwing,
    val hinge: HingeSide
)

data class OpeningInputErrors(
    val offset: DimensionError?,
    val width: DimensionError?,
    val height: DimensionError?,
    val sillHeight: DimensionError?,
    val layout: OpeningLayoutError?
) {
    val hasErrors: Boolean
        get() = offset != null || width != null || height != null ||
            sillHeight != null || layout != null
}

sealed interface OpeningValidation {
    data class Valid(
        val side: WallSide,
        val type: OpeningType,
        val offsetM: Double,
        val widthM: Double,
        val heightM: Double,
        val sillHeightM: Double,
        val swing: DoorSwing?,
        val hinge: HingeSide?
    ) : OpeningValidation

    data class Invalid(val errors: OpeningInputErrors) : OpeningValidation
}

object OpeningValidator {

    /**
     * Proverava vrata ili prozor na zidu sobe.
     * [others] su ostali otvori te sobe (bez otvora koji se menja).
     * Otvori koji se samo dodiruju krajevima ne računaju se kao preklopljeni.
     */
    fun validate(input: OpeningInput, room: Room, others: List<Opening>): OpeningValidation {
        val isDoor = input.type == OpeningType.DOOR

        val offset = MeasureParser.parse(input.offset, 0.0, OpeningLimits.MAX_OFFSET_M)
        val width = if (isDoor) {
            MeasureParser.parse(input.width, OpeningLimits.DOOR_MIN_WIDTH_M, OpeningLimits.DOOR_MAX_WIDTH_M)
        } else {
            MeasureParser.parse(input.width, OpeningLimits.WINDOW_MIN_WIDTH_M, OpeningLimits.WINDOW_MAX_WIDTH_M)
        }
        val height = if (isDoor) {
            MeasureParser.parse(input.height, OpeningLimits.DOOR_MIN_HEIGHT_M, OpeningLimits.DOOR_MAX_HEIGHT_M)
        } else {
            MeasureParser.parse(input.height, OpeningLimits.WINDOW_MIN_HEIGHT_M, OpeningLimits.WINDOW_MAX_HEIGHT_M)
        }
        // Vrata uvek počinju od poda, pa se tekst za visinu donje ivice ignoriše.
        val sill: MeasureResult? = if (isDoor) {
            null
        } else {
            MeasureParser.parse(input.sillHeight, OpeningLimits.WINDOW_MIN_SILL_M, OpeningLimits.WINDOW_MAX_SILL_M)
        }

        val fieldErrors = OpeningInputErrors(
            offset = (offset as? MeasureResult.Failure)?.error,
            width = (width as? MeasureResult.Failure)?.error,
            height = (height as? MeasureResult.Failure)?.error,
            sillHeight = (sill as? MeasureResult.Failure)?.error,
            layout = null
        )
        if (fieldErrors.hasErrors) return OpeningValidation.Invalid(fieldErrors)

        val offsetM = (offset as MeasureResult.Value).meters
        val widthM = (width as MeasureResult.Value).meters
        val heightM = (height as MeasureResult.Value).meters
        val sillM = (sill as? MeasureResult.Value)?.meters ?: 0.0

        val layout = layoutError(input.side, offsetM, widthM, heightM, sillM, room, others)
        if (layout != null) {
            return OpeningValidation.Invalid(fieldErrors.copy(layout = layout))
        }

        return OpeningValidation.Valid(
            side = input.side,
            type = input.type,
            offsetM = offsetM,
            widthM = widthM,
            heightM = heightM,
            sillHeightM = sillM,
            swing = if (isDoor) input.swing else null,
            hinge = if (isDoor) input.hinge else null
        )
    }

    /**
     * Da li otvor staje u zid i u visinu sobe, i da li se preklapa sa drugim otvorima na istom zidu.
     * Koristi se i pri unosu otvora i pri proveri da li otvor i dalje staje posle izmene mera sobe.
     */
    fun layoutError(
        side: WallSide,
        offsetM: Double,
        widthM: Double,
        heightM: Double,
        sillHeightM: Double,
        room: Room,
        others: List<Opening>
    ): OpeningLayoutError? {
        val eps = RoomGeometry.EPSILON_M
        if (offsetM + widthM > room.wallLengthM(side) + eps) return OpeningLayoutError.OUT_OF_WALL
        if (sillHeightM + heightM > room.heightM + eps) return OpeningLayoutError.TOO_HIGH_FOR_ROOM

        val overlaps = others.any { other ->
            other.roomId == room.id && other.side == side &&
                offsetM < other.endM - eps && other.offsetM < offsetM + widthM - eps
        }
        if (overlaps) return OpeningLayoutError.OVERLAPS_OTHER
        return null
    }
}
