package com.zoran.aihomedesigner.domain

object RoomGeometry {

    /**
     * Numerička tolerancija pri poređenju decimalnih brojeva.
     * Ovo NIJE tačnost merenja i ne tretira se kao dozvoljena greška mere.
     */
    const val EPSILON_M = 1e-6

    /**
     * Da li se unutrašnjosti dve sobe preklapaju. Sobe koje se samo dodiruju ivicom
     * se ne računaju kao preklopljene. Soba bez položaja se ne poredi.
     * Debljina zidova se ovde ne uzima u obzir.
     */
    fun overlaps(a: Room, b: Room): Boolean {
        val ax = a.xM ?: return false
        val ay = a.yM ?: return false
        val bx = b.xM ?: return false
        val by = b.yM ?: return false

        return ax < bx + b.lengthM - EPSILON_M &&
            bx < ax + a.lengthM - EPSILON_M &&
            ay < by + b.widthM - EPSILON_M &&
            by < ay + a.widthM - EPSILON_M
    }

    /** Identifikatori svih soba koje se preklapaju sa bar još jednom sobom. */
    fun overlappingRoomIds(rooms: List<Room>): Set<String> {
        val ids = mutableSetOf<String>()
        for (i in rooms.indices) {
            for (j in i + 1 until rooms.size) {
                if (overlaps(rooms[i], rooms[j])) {
                    ids += rooms[i].id
                    ids += rooms[j].id
                }
            }
        }
        return ids
    }
}
