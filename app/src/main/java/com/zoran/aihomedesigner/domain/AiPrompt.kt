package com.zoran.aihomedesigner.domain

import java.util.Locale

/**
 * Tekstualni opis nekretnine za AI servis koji pravi slike. Pravi se samo iz podataka u modelu
 * (sobe sa položajem, vrata i prozori, nameštaj, stepenice), bez izmišljenih mera. Opis je na engleskom
 * jer ga tako servisi za slike najbolje razumeju. Slika dobijena iz ovog opisa je samo ilustracija:
 * mere i raspored se uvek čitaju iz modela, ne sa slike.
 */
object AiPrompt {

    /** Najviše znakova imena sobe ili nameštaja koje ulazi u opis. */
    const val MAX_NAME_CHARS = 60

    /** Vraća opis, ili null ako nijedna soba nema položaj na planu. */
    fun build(
        rooms: List<Room>,
        openings: List<Opening>,
        furniture: List<Furniture>,
        stairs: List<Stairs> = emptyList()
    ): String? {
        val placed = rooms.filter { it.isPlaced }
        if (placed.isEmpty()) return null

        val lines = mutableListOf<String>()
        lines += "Create one realistic architectural visualization of a home, as a 3D cutaway view from above " +
            "with roofs and ceilings removed so that every room is visible. Use the layout described below. " +
            "In the plan, north is the top of the page and east is to the right. All sizes are in metres. " +
            "This is an illustration only: do not add any text, labels, dimensions or watermarks to the image."

        val levels = placed.map { it.floorLevel }.distinct().sorted()
        for (level in levels) {
            lines += ""
            lines += "${levelName(level).replaceFirstChar { it.uppercase() }}:"
            val levelRooms = placed.filter { it.floorLevel == level }.sortedWith(compareBy({ it.yM }, { it.xM }))
            for (room in levelRooms) {
                lines += "- Room \"${clean(room.name)}\": ${fmt(room.lengthM)} m east-west by ${fmt(room.widthM)} m " +
                    "north-south, ceiling height ${fmt(room.heightM)} m, its north-west corner is " +
                    "${fmt(room.xM!!)} m east and ${fmt(room.yM!!)} m south of the plan origin."
                for (opening in openings.filter { it.roomId == room.id }.sortedBy { it.side }) {
                    lines += "  - " + openingLine(opening)
                }
                for (item in furniture.filter { it.roomId == room.id }) {
                    lines += "  - " + furnitureLine(item)
                }
            }
        }

        val drawn = stairs.filter { it.lowerLevel in levels || it.upperLevel in levels }
        if (drawn.isNotEmpty()) {
            lines += ""
            lines += "Stairs:"
            for (item in drawn) lines += "- " + stairsLine(item)
        }

        lines += ""
        lines += "Keep the proportions of the rooms and the positions of doors, windows, furniture and stairs " +
            "as close to this description as possible. Use a neutral, bright, realistic style."
        return lines.joinToString("\n")
    }

    private fun openingLine(o: Opening): String {
        val wall = o.side.name.lowercase()
        val from = if (o.side == WallSide.NORTH || o.side == WallSide.SOUTH) "west" else "north"
        val kind = if (o.type == OpeningType.DOOR) "door" else "window"
        val sill = if (o.type == OpeningType.WINDOW) ", sill ${fmt(o.sillHeightM)} m above the floor" else ""
        return "$kind on the $wall wall, ${fmt(o.widthM)} m wide and ${fmt(o.heightM)} m high, " +
            "starting ${fmt(o.offsetM)} m from the $from end of that wall$sill."
    }

    private fun furnitureLine(f: Furniture): String =
        "furniture \"${clean(f.name)}\" (${f.category.name.lowercase()}): ${fmt(f.footprintLengthM)} m east-west by " +
            "${fmt(f.footprintWidthM)} m north-south, ${fmt(f.heightM)} m high, its north-west corner is " +
            "${fmt(f.xM)} m east and ${fmt(f.yM)} m south of the room's north-west corner."

    private fun stairsLine(s: Stairs): String {
        val f = StairsGeometry.footprint(s)
        val toward = when (s.directionDeg) {
            0 -> "east"
            90 -> "south"
            180 -> "west"
            270 -> "north"
            else -> "an unspecified direction"
        }
        return "straight stairs from ${levelName(s.lowerLevel)} up to ${levelName(s.upperLevel)}, climbing toward " +
            "the $toward, footprint ${fmt(f.x1 - f.x0)} m east-west by ${fmt(f.y1 - f.y0)} m north-south, " +
            "its north-west corner is ${fmt(f.x0)} m east and ${fmt(f.y0)} m south of the plan origin."
    }

    private fun levelName(level: Int): String = when {
        level == 0 -> "ground floor"
        level > 0 -> "floor $level"
        else -> "basement ${-level}"
    }

    /** Ime iz modela ide u opis bez znakova koji bi razbili njegovu strukturu. */
    private fun clean(name: String): String {
        val cleaned = name.map { if (it.isISOControl() || it == '"') ' ' else it }
            .joinToString("").trim().replace(Regex("\\s+"), " ").take(MAX_NAME_CHARS).trim()
        return cleaned.ifEmpty { "unnamed" }
    }

    private fun fmt(value: Double): String {
        val text = String.format(Locale.US, "%.2f", value)
        val trimmed = if (text.contains('.')) text.trimEnd('0').trimEnd('.') else text
        return if (trimmed == "-0") "0" else trimmed
    }
}
