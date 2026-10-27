package com.zoran.aihomedesigner.domain

import kotlin.math.abs
import kotlin.math.hypot

/**
 * Zid sobe koji je korisnik dodirnuo na planu. [alongM] je mesto dodira duž zida, mereno od početka zida
 * (zapadni ugao za severni i južni zid, severni ugao za istočni i zapadni, kao kod [Opening.offsetM]),
 * a [distanceM] je koliko je dodir bio daleko od zida.
 */
data class WallTap(val roomId: String, val side: WallSide, val alongM: Double, val distanceM: Double)

/** Pronalaženje zida sobe koji je korisnik dodirnuo na planu (čista logika, bez Androida). */
object WallPicking {

    private const val EPS = 1e-6

    /**
     * Najbliži zid bilo koje sobe tački ([xM], [yM]) u koordinatama plana, ako je dalji od zida najviše
     * [toleranceM]. Zidovi su ivice pravougaonika soba. Sobe bez položaja se preskaču.
     * Kod zida koji dele dve sobe, bira se soba u čijoj je unutrašnjosti dodir, jer su im zidovi na istoj liniji.
     */
    fun nearest(rooms: List<Room>, xM: Double, yM: Double, toleranceM: Double): WallTap? {
        var best: WallTap? = null
        var bestInside = false
        for (room in rooms) {
            val rx = room.xM ?: continue
            val ry = room.yM ?: continue
            val inside = xM >= rx && xM <= rx + room.lengthM && yM >= ry && yM <= ry + room.widthM
            for (side in WallSide.values()) {
                val tap = project(room, rx, ry, side, xM, yM)
                if (tap.distanceM > toleranceM) continue
                val current = best
                val better = current == null ||
                    tap.distanceM < current.distanceM - EPS ||
                    (abs(tap.distanceM - current.distanceM) <= EPS && inside && !bestInside)
                if (better) {
                    best = tap
                    bestInside = inside
                }
            }
        }
        return best
    }

    /** Najbliža tačka na zidu [side] sobe i rastojanje do nje. */
    private fun project(room: Room, rx: Double, ry: Double, side: WallSide, x: Double, y: Double): WallTap {
        val along: Double
        val cx: Double
        val cy: Double
        when (side) {
            WallSide.NORTH -> {
                along = (x - rx).coerceIn(0.0, room.lengthM)
                cx = rx + along
                cy = ry
            }
            WallSide.SOUTH -> {
                along = (x - rx).coerceIn(0.0, room.lengthM)
                cx = rx + along
                cy = ry + room.widthM
            }
            WallSide.WEST -> {
                along = (y - ry).coerceIn(0.0, room.widthM)
                cx = rx
                cy = ry + along
            }
            WallSide.EAST -> {
                along = (y - ry).coerceIn(0.0, room.widthM)
                cx = rx + room.lengthM
                cy = ry + along
            }
        }
        return WallTap(room.id, side, along, hypot(x - cx, y - cy))
    }
}
