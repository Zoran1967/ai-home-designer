package com.zoran.aihomedesigner.domain

sealed interface FurnitureIssue {
    /** Predmet izlazi iz unutrašnjosti sobe. */
    data class OutsideRoom(val furnitureId: String) : FurnitureIssue

    /** Predmet je viši od sobe. */
    data class TooTall(val furnitureId: String) : FurnitureIssue

    /** Dva predmeta se preklapaju na podu. */
    data class Overlap(val furnitureAId: String, val furnitureBId: String) : FurnitureIssue

    /** Predmet ulazi u prolaz ispred vrata ili u površinu koju vrata prebrišu pri otvaranju. */
    data class BlocksDoor(val furnitureId: String, val openingId: String) : FurnitureIssue
}

/**
 * Provera rasporeda nameštaja u jednoj sobi. Proverava SAMO: da li predmeti izlaze iz sobe, da li su viši
 * od sobe, da li se preklapaju i da li blokiraju vrata. Ne proverava širinu prolaza između predmeta,
 * prozore, ni nameštaj u drugim sobama. Prazan spisak znači samo da ove provere nisu našle problem.
 */
object FurnitureLayoutValidator {

    fun validate(room: Room, furniture: List<Furniture>, openings: List<Opening>): List<FurnitureIssue> {
        val issues = mutableListOf<FurnitureIssue>()

        for (item in furniture) {
            if (!FurnitureGeometry.isInsideRoom(room, item)) issues += FurnitureIssue.OutsideRoom(item.id)
            if (item.heightM > room.heightM + RoomGeometry.EPSILON_M) issues += FurnitureIssue.TooTall(item.id)
        }

        for (i in furniture.indices) {
            for (j in i + 1 until furniture.size) {
                val a = FurnitureGeometry.rect(furniture[i])
                val b = FurnitureGeometry.rect(furniture[j])
                if (FurnitureGeometry.intersects(a, b)) {
                    issues += FurnitureIssue.Overlap(furniture[i].id, furniture[j].id)
                }
            }
        }

        for (item in furniture) {
            for (opening in openings) {
                if (FurnitureGeometry.blocksDoor(room, item, opening)) {
                    issues += FurnitureIssue.BlocksDoor(item.id, opening.id)
                }
            }
        }
        return issues
    }
}
