package com.zoran.aihomedesigner.domain

/** Tačka u 3D prostoru, u metrima: x udesno (kao na planu), y nadole na planu (ka jugu), z naviše. */
data class Vec3(val x: Double, val y: Double, val z: Double)

enum class SceneKind { FLOOR, WALL, DOOR, WINDOW, FURNITURE }

/**
 * Pravougaoni kvadar poravnat sa osama, u metrima. Sve što se crta u 3D prikazu pravi se iz istih
 * podataka kao i 2D plan (sobe, vrata i prozori, nameštaj), bez posebne kopije modela.
 */
data class SceneBox(
    val min: Vec3,
    val max: Vec3,
    val kind: SceneKind,
    val label: String? = null,
    val itemId: String? = null
)

object SceneBuilder {

    /**
     * Debljina zida koja se crta kad za sobu nije uneta debljina zida (10 cm).
     * To je samo izgled u prikazu, ne mera, i ne vodi se kao uneta debljina.
     */
    const val VISUAL_WALL_M = 0.1

    /** Debljina ploče poda u prikazu. */
    const val SLAB_M = 0.05

    /** Koliko vrata i prozori "vire" iz zida sa svake strane, da se vide uz zid. */
    const val OPENING_PROUD_M = 0.01

    fun wallThicknessForDisplay(room: Room): Double = room.wallThicknessM ?: VISUAL_WALL_M

    /** Debljina međuspratne konstrukcije u prikazu (25 cm). Samo izgled, nije izmerena vrednost. */
    const val FLOOR_SLAB_M = 0.25

    /** Visina sprata koji nema nijednu sobu, a nalazi se između drugih spratova (samo za prikaz). */
    const val DEFAULT_FLOOR_HEIGHT_M = 2.6

    /**
     * Visina (z) na kojoj počinje svaki sprat u 3D prikazu: prizemlje je na 0, svaki viši sprat počinje iznad
     * prethodnog (visina najviše sobe tog sprata plus međuspratna konstrukcija), a podrumi su ispod nule.
     */
    fun floorOffsets(rooms: List<Room>): Map<Int, Double> {
        val placed = rooms.filter { it.isPlaced }
        val heightByLevel = placed.groupBy { it.floorLevel }.mapValues { (_, list) -> list.maxOf { it.heightM } }
        val levels = Floors.levels(placed)
        val offsets = mutableMapOf(0 to 0.0)
        for (level in 1..levels.max()) {
            val below = level - 1
            offsets[level] = offsets.getValue(below) + (heightByLevel[below] ?: DEFAULT_FLOOR_HEIGHT_M) + FLOOR_SLAB_M
        }
        for (level in -1 downTo levels.min()) {
            offsets[level] = offsets.getValue(level + 1) - ((heightByLevel[level] ?: DEFAULT_FLOOR_HEIGHT_M) + FLOOR_SLAB_M)
        }
        return offsets
    }

    /**
     * Pravi kvadre za sve sobe sa položajem na planu, njihova vrata i prozore i nameštaj.
     * Sobe sa različitih spratova stoje jedna iznad druge.
     */
    fun build(rooms: List<Room>, openings: List<Opening>, furniture: List<Furniture>): List<SceneBox> {
        val placed = rooms.filter { it.isPlaced }
        val roomsById = placed.associateBy { it.id }
        val offsets = floorOffsets(placed)
        val result = mutableListOf<SceneBox>()

        for (room in placed) {
            val rx = room.xM!!
            val ry = room.yM!!
            val z0 = offsets.getValue(room.floorLevel)
            val t = wallThicknessForDisplay(room)
            val len = room.lengthM
            val wid = room.widthM
            val h = room.heightM

            result += SceneBox(Vec3(rx, ry, z0 - SLAB_M), Vec3(rx + len, ry + wid, z0), SceneKind.FLOOR, room.name, room.id)
            result += SceneBox(Vec3(rx - t, ry - t, z0), Vec3(rx + len + t, ry, z0 + h), SceneKind.WALL, room.name, room.id)
            result += SceneBox(Vec3(rx - t, ry + wid, z0), Vec3(rx + len + t, ry + wid + t, z0 + h), SceneKind.WALL, room.name, room.id)
            result += SceneBox(Vec3(rx - t, ry, z0), Vec3(rx, ry + wid, z0 + h), SceneKind.WALL, room.name, room.id)
            result += SceneBox(Vec3(rx + len, ry, z0), Vec3(rx + len + t, ry + wid, z0 + h), SceneKind.WALL, room.name, room.id)
        }

        for (item in furniture) {
            val room = roomsById[item.roomId] ?: continue
            val z0 = offsets.getValue(room.floorLevel)
            val x0 = room.xM!! + item.xM
            val y0 = room.yM!! + item.yM
            result += SceneBox(
                Vec3(x0, y0, z0),
                Vec3(x0 + item.footprintLengthM, y0 + item.footprintWidthM, z0 + item.heightM),
                SceneKind.FURNITURE,
                item.name,
                item.id
            )
        }

        for (opening in openings) {
            val room = roomsById[opening.roomId] ?: continue
            result += openingBox(room, opening, offsets.getValue(room.floorLevel))
        }
        return result
    }

    /** Vrata ili prozor kao ploča koja prolazi kroz zid i malo viri sa obe strane. */
    private fun openingBox(room: Room, opening: Opening, floorZ: Double): SceneBox {
        val rx = room.xM!!
        val ry = room.yM!!
        val t = wallThicknessForDisplay(room)
        val p = OPENING_PROUD_M
        val a = opening.offsetM
        val b = opening.offsetM + opening.widthM
        val zMin = floorZ + if (opening.type == OpeningType.DOOR) 0.0 else opening.sillHeightM
        val zMax = zMin + opening.heightM
        val kind = if (opening.type == OpeningType.DOOR) SceneKind.DOOR else SceneKind.WINDOW

        val (min, max) = when (opening.side) {
            WallSide.NORTH -> Vec3(rx + a, ry - t - p, zMin) to Vec3(rx + b, ry + p, zMax)
            WallSide.SOUTH -> Vec3(rx + a, ry + room.widthM - p, zMin) to Vec3(rx + b, ry + room.widthM + t + p, zMax)
            WallSide.WEST -> Vec3(rx - t - p, ry + a, zMin) to Vec3(rx + p, ry + b, zMax)
            WallSide.EAST -> Vec3(rx + room.lengthM - p, ry + a, zMin) to Vec3(rx + room.lengthM + t + p, ry + b, zMax)
        }
        return SceneBox(min, max, kind, null, opening.id)
    }
}
