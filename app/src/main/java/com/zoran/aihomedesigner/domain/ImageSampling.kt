package com.zoran.aihomedesigner.domain

import kotlin.math.max

object ImageSampling {

    /**
     * Stepen dvojke za koji treba smanjiti sliku pri učitavanju da duža strana ne pređe [maxSidePx].
     * Služi samo za prikaz (da se izbegne prevelika upotreba memorije). Sačuvana slika ostaje u punoj veličini.
     */
    fun sampleSize(widthPx: Int, heightPx: Int, maxSidePx: Int): Int {
        require(maxSidePx > 0) { "maxSidePx mora biti veći od nule" }
        var sample = 1
        val longSide = max(widthPx, heightPx)
        while (longSide / sample > maxSidePx) sample *= 2
        return sample
    }
}
