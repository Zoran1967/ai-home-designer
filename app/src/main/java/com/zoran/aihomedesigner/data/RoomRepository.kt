package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomInput
import com.zoran.aihomedesigner.domain.RoomInputErrors
import com.zoran.aihomedesigner.domain.RoomValidation
import com.zoran.aihomedesigner.domain.RoomValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface RoomSaveResult {
    data class Success(val room: Room) : RoomSaveResult
    data class Invalid(val errors: RoomInputErrors) : RoomSaveResult

    /** Soba koju menjamo više ne postoji (npr. obrisana je). */
    data object NotFound : RoomSaveResult
}

class RoomRepository(private val dao: RoomDao) {

    fun observeRooms(projectId: String): Flow<List<Room>> =
        dao.observeByProject(projectId).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    /** Proverava unos, pa tek onda upisuje sobu. Mere koje je korisnik upisao vode se kao USER_CONFIRMED. */
    suspend fun createRoom(
        projectId: String,
        input: RoomInput,
        nowMillis: Long = System.currentTimeMillis()
    ): RoomSaveResult {
        val validation = RoomValidator.validate(input, dao.getNamesByProject(projectId))
        return when (validation) {
            is RoomValidation.Invalid -> RoomSaveResult.Invalid(validation.errors)
            is RoomValidation.Valid -> {
                val room = Room(
                    id = Room.newId(),
                    projectId = projectId,
                    name = validation.name,
                    lengthM = validation.lengthM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    wallThicknessM = validation.wallThicknessM,
                    dimensionSource = DimensionSource.USER_CONFIRMED,
                    createdAtMillis = nowMillis
                )
                dao.insert(room.toEntity())
                RoomSaveResult.Success(room)
            }
        }
    }

    /**
     * Menja ime i mere postojeće sobe. Mere se menjaju samo onim što je korisnik upisao.
     * Ako se mere nisu promenile, izvor mera ostaje isti. Ako jesu, vode se kao USER_CONFIRMED.
     */
    suspend fun updateRoom(roomId: String, input: RoomInput): RoomSaveResult {
        val existing = dao.getById(roomId)?.toDomainOrNull() ?: return RoomSaveResult.NotFound
        val otherNames = dao.getNamesByProjectExcluding(existing.projectId, roomId)

        return when (val validation = RoomValidator.validate(input, otherNames)) {
            is RoomValidation.Invalid -> RoomSaveResult.Invalid(validation.errors)
            is RoomValidation.Valid -> {
                val dimensionsChanged =
                    validation.lengthM != existing.lengthM ||
                        validation.widthM != existing.widthM ||
                        validation.heightM != existing.heightM ||
                        validation.wallThicknessM != existing.wallThicknessM
                val updated = existing.copy(
                    name = validation.name,
                    lengthM = validation.lengthM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    wallThicknessM = validation.wallThicknessM,
                    dimensionSource = if (dimensionsChanged) {
                        DimensionSource.USER_CONFIRMED
                    } else {
                        existing.dimensionSource
                    }
                )
                dao.update(updated.toEntity())
                RoomSaveResult.Success(updated)
            }
        }
    }

    suspend fun deleteRoom(id: String) = dao.deleteById(id)
}
