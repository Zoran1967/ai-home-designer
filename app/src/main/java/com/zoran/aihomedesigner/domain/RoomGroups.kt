package com.zoran.aihomedesigner.domain

import java.util.UUID

/** Ishod pokušaja pomeranja sobe (zajedno sa sobama zalepljenim za nju). */
sealed interface RoomMoveResult {
    /** Nove pozicije za sve sobe koje se pomeraju (identifikator sobe -> položaj na planu). */
    data class Moved(val positions: Map<String, PlanPoint>) : RoomMoveResult

    /** Pomeranje nije dozvoljeno jer je soba (ili neka soba zalepljena za nju) zaključana. */
    data class Blocked(val lockedRoomId: String) : RoomMoveResult

    /** Soba nema položaj na planu, pa se ne može pomeriti. */
    data object NotPlaced : RoomMoveResult
}

/** Ishod lepljenja sobe uz susedne sobe. */
sealed interface RoomGlueResult {
    /** Sobe su zalepljene: svakoj sobi iz [assignments] se postavlja isti [groupId]. */
    data class Glued(
        val groupId: String,
        val assignments: Map<String, String>,
        val neighbourIds: List<String>
    ) : RoomGlueResult

    /** Soba nema susedne sobe (zid između soba) na istom spratu. */
    data object NoNeighbours : RoomGlueResult
}

/** Zaključavanje i lepljenje soba na planu. Čista logika, bez Androida. */
object RoomGroups {

    /** Soba i sobe zalepljene za nju (istog sprata). Ako nije zalepljena ni za jednu, samo ona. */
    fun members(rooms: List<Room>, roomId: String): List<Room> {
        val room = rooms.firstOrNull { it.id == roomId } ?: return emptyList()
        val groupId = room.glueGroupId ?: return listOf(room)
        val group = rooms.filter { it.glueGroupId == groupId && it.floorLevel == room.floorLevel }
        return if (group.size >= 2) group else listOf(room)
    }

    fun isGlued(rooms: List<Room>, roomId: String): Boolean = members(rooms, roomId).size >= 2

    /**
     * Planira pomeranje sobe [roomId] na (newX, newY). Sobe zalepljene za nju se pomeraju za isti pomak,
     * pa ostaju na istom rastojanju jedna od druge. Ako je bilo koja od tih soba zaključana, ništa se ne pomera.
     */
    fun planMove(rooms: List<Room>, roomId: String, newX: Double, newY: Double): RoomMoveResult {
        val room = rooms.firstOrNull { it.id == roomId } ?: return RoomMoveResult.NotPlaced
        val x = room.xM ?: return RoomMoveResult.NotPlaced
        val y = room.yM ?: return RoomMoveResult.NotPlaced

        val group = members(rooms, roomId)
        group.firstOrNull { it.positionLocked }?.let { return RoomMoveResult.Blocked(it.id) }

        val dx = newX - x
        val dy = newY - y
        val positions = group.filter { it.isPlaced }.associate {
            it.id to PlanPoint(
                RoomPlacement.roundToMm(it.xM!! + dx),
                RoomPlacement.roundToMm(it.yM!! + dy)
            )
        }
        return RoomMoveResult.Moved(positions)
    }

    /**
     * Lepi sobu [roomId] uz sve susedne sobe istog sprata (sobe sa kojima deli zid). Ako su neke od njih već u
     * grupama, grupe se spajaju u jednu.
     */
    fun glueWithNeighbours(
        rooms: List<Room>,
        roomId: String,
        newGroupId: () -> String = { UUID.randomUUID().toString() }
    ): RoomGlueResult {
        val room = rooms.firstOrNull { it.id == roomId && it.isPlaced } ?: return RoomGlueResult.NoNeighbours
        val sameFloor = rooms.filter { it.isPlaced && it.floorLevel == room.floorLevel }

        val neighbourIds = SharedWalls.find(sameFloor).mapNotNull { wall ->
            when (roomId) {
                wall.roomAId -> wall.roomBId
                wall.roomBId -> wall.roomAId
                else -> null
            }
        }.distinct()
        if (neighbourIds.isEmpty()) return RoomGlueResult.NoNeighbours

        val involvedIds = listOf(roomId) + neighbourIds
        val involved = sameFloor.filter { it.id in involvedIds }
        val existingGroups = involved.mapNotNull { it.glueGroupId }.distinct()
        val groupId = existingGroups.firstOrNull() ?: newGroupId()

        val affected = sameFloor.filter { it.id in involvedIds || (it.glueGroupId != null && it.glueGroupId in existingGroups) }
        return RoomGlueResult.Glued(groupId, affected.associate { it.id to groupId }, neighbourIds)
    }

    /**
     * Odlepljuje sobu [roomId] od grupe. Ako u grupi ostane samo jedna soba, i ona se odlepljuje.
     * Vraća nove vrednosti glueGroupId (null = nije zalepljena) samo za sobe koje se menjaju.
     */
    fun unglue(rooms: List<Room>, roomId: String): Map<String, String?> {
        val room = rooms.firstOrNull { it.id == roomId } ?: return emptyMap()
        val groupId = room.glueGroupId ?: return emptyMap()
        val others = rooms.filter { it.glueGroupId == groupId && it.id != roomId }

        val result = mutableMapOf<String, String?>(roomId to null)
        if (others.size <= 1) others.forEach { result[it.id] = null }
        return result
    }
}
