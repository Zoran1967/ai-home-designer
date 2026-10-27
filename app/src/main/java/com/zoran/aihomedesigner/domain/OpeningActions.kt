package com.zoran.aihomedesigner.domain

import kotlin.math.hypot
import kotlin.math.max

/** Brze izmene vrata (okretanje otvaranja i šarki) i pronalaženje vrata ili prozora dodirnutih na planu. */
object OpeningActions {

    /** Samo vrata koja se otvaraju (ne klizna) imaju smer otvaranja i šarke koje se mogu okrenuti. */
    fun canFlip(opening: Opening): Boolean =
        opening.type == OpeningType.DOOR && opening.swing != null && opening.swing != DoorSwing.SLIDING

    /** Unos za iste otvor, samo sa suprotnim smerom otvaranja (ka sobi <-> od sobe). Null ako se ne može okrenuti. */
    fun flipSwing(opening: Opening): OpeningInput? {
        if (!canFlip(opening)) return null
        val swing = if (opening.swing == DoorSwing.INTO_ROOM) DoorSwing.OUT_OF_ROOM else DoorSwing.INTO_ROOM
        return input(opening, swing, opening.hinge)
    }

    /** Unos za iste otvor, samo sa šarkama na suprotnom kraju. Null ako se ne mogu okrenuti. */
    fun flipHinge(opening: Opening): OpeningInput? {
        if (!canFlip(opening)) return null
        val hinge = when (opening.hinge) {
            HingeEnd.START -> HingeEnd.END
            HingeEnd.END -> HingeEnd.START
            null -> return null
        }
        return input(opening, opening.swing, hinge)
    }

    /** Unos koji ponavlja sve mere otvora, uz zadati smer otvaranja i šarke. Mere se vraćaju u iste vrednosti. */
    fun input(opening: Opening, swing: DoorSwing?, hinge: HingeEnd?): OpeningInput = OpeningInput(
        type = opening.type,
        side = opening.side,
        offset = RoomMeasureEdit.inputText(opening.offsetM),
        width = RoomMeasureEdit.inputText(opening.widthM),
        height = RoomMeasureEdit.inputText(opening.heightM),
        sillHeight = if (opening.type == OpeningType.WINDOW) RoomMeasureEdit.inputText(opening.sillHeightM) else "",
        swing = swing,
        hinge = hinge
    )

    /**
     * Najbliži otvor tački ([xM], [yM]) na planu, ako je dalji od njega (od linije otvora na zidu) najviše
     * [toleranceM]. Otvori u sobama bez položaja ili bez sobe se preskaču.
     */
    fun nearestOpening(
        rooms: List<Room>,
        openings: List<Opening>,
        xM: Double,
        yM: Double,
        toleranceM: Double
    ): Opening? {
        val roomsById = rooms.associateBy { it.id }
        var best: Opening? = null
        var bestDistance = Double.MAX_VALUE
        for (opening in openings) {
            val room = roomsById[opening.roomId] ?: continue
            val rx = room.xM ?: continue
            val ry = room.yM ?: continue
            val x0: Double
            val y0: Double
            val x1: Double
            val y1: Double
            when (opening.side) {
                WallSide.NORTH -> { x0 = rx + opening.offsetM; x1 = x0 + opening.widthM; y0 = ry; y1 = ry }
                WallSide.SOUTH -> { x0 = rx + opening.offsetM; x1 = x0 + opening.widthM; y0 = ry + room.widthM; y1 = y0 }
                WallSide.WEST -> { x0 = rx; x1 = rx; y0 = ry + opening.offsetM; y1 = y0 + opening.widthM }
                WallSide.EAST -> { x0 = rx + room.lengthM; x1 = x0; y0 = ry + opening.offsetM; y1 = y0 + opening.widthM }
            }
            // Zidovi su vodoravni ili uspravni, pa je rastojanje do duži zbir van-opsega po svakoj osi
            val dx = max(max(x0 - xM, 0.0), xM - x1)
            val dy = max(max(y0 - yM, 0.0), yM - y1)
            val distance = hypot(dx, dy)
            if (distance <= toleranceM && distance < bestDistance) {
                best = opening
                bestDistance = distance
            }
        }
        return best
    }
}
