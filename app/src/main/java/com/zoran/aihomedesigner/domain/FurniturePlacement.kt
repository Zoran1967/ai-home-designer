package com.zoran.aihomedesigner.domain

object FurniturePlacement {

    /** Korak pretrage slobodnog mesta u sobi (10 cm). */
    private const val SEARCH_STEP_M = 0.1

    /**
     * Prvo slobodno mesto za novi predmet, tražeći red po red od gornjeg levog ugla sobe:
     * predmet mora da stane u sobu, da se ne preklapa sa drugim nameštajem i da ne blokira vrata.
     * Vraća položaj u koordinatama sobe, ili null ako nema slobodnog mesta.
     * Ovo je samo početni položaj. Korisnik predmet kasnije postavlja na pravo mesto.
     */
    fun firstFreeSpot(
        room: Room,
        existing: List<Furniture>,
        footprintLengthM: Double,
        footprintWidthM: Double,
        openings: List<Opening>
    ): PlanPoint? {
        val maxX = room.lengthM - footprintLengthM
        val maxY = room.widthM - footprintWidthM
        if (maxX < -RoomGeometry.EPSILON_M || maxY < -RoomGeometry.EPSILON_M) return null

        val others = existing.map { FurnitureGeometry.rect(it) }
        val steps = SEARCH_STEP_M

        var iy = 0
        while (iy * steps <= maxY + RoomGeometry.EPSILON_M) {
            val y = iy * steps
            var ix = 0
            while (ix * steps <= maxX + RoomGeometry.EPSILON_M) {
                val x = ix * steps
                val candidate = Furniture(
                    id = "candidate",
                    roomId = room.id,
                    name = "",
                    category = FurnitureCategory.OTHER,
                    lengthM = footprintLengthM,
                    widthM = footprintWidthM,
                    heightM = 0.1,
                    xM = x,
                    yM = y,
                    rotationDeg = 0,
                    dimensionSource = DimensionSource.ESTIMATED,
                    createdAtMillis = 0L
                )
                val rect = FurnitureGeometry.rect(candidate)
                val free = others.none { FurnitureGeometry.intersects(rect, it) } &&
                    openings.none { FurnitureGeometry.blocksDoor(room, candidate, it) }
                if (free) return PlanPoint(x, y)
                ix++
            }
            iy++
        }
        return null
    }
}
