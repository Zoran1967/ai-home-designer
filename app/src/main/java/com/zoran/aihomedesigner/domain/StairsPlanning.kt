package com.zoran.aihomedesigner.domain

/** Pomoćna logika za stepenice koja zavisi od soba projekta (visina uspona, početni položaj). */
object StairsPlanning {

    /**
     * Procena visine uspona: od poda sprata [lowerLevel] do poda sprata iznad.
     * Računa se kao visina najvišeg zida na donjem spratu plus debljina međuspratne ploče
     * ([SceneBuilder.FLOOR_SLAB_M]). Ako na spratu nema soba, uzima se [SceneBuilder.DEFAULT_FLOOR_HEIGHT_M].
     * To je procena za prikaz i savete, a ne izmerena visina.
     */
    fun riseM(rooms: List<Room>, lowerLevel: Int): Double {
        val height = rooms.filter { it.floorLevel == lowerLevel }.maxOfOrNull { it.heightM }
            ?: SceneBuilder.DEFAULT_FLOOR_HEIGHT_M
        return height + SceneBuilder.FLOOR_SLAB_M
    }

    /**
     * Početni položaj novih stepenica: gornji levi ugao prve sobe koja ima položaj na donjem spratu,
     * a ako takve nema, početak plana (0, 0). Korisnik ga posle pomera.
     */
    fun defaultSpot(rooms: List<Room>, lowerLevel: Int): PlanPoint {
        val room = rooms.firstOrNull { it.floorLevel == lowerLevel && it.xM != null && it.yM != null }
        return if (room == null) PlanPoint(0.0, 0.0) else PlanPoint(room.xM!!, room.yM!!)
    }
}
