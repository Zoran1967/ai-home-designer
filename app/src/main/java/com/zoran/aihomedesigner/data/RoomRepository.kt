package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomInput
import com.zoran.aihomedesigner.domain.RoomInputErrors
import com.zoran.aihomedesigner.domain.RoomValidation
import com.zoran.aihomedesigner.domain.RoomValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface CreateRoomResult {
    data class Success(val room: Room) : CreateRoomResult
    data class Invalid(val errors: RoomInputErrors) : CreateRoomResult
}

class RoomRepository(private val dao: RoomDao) {

    fun observeRooms(projectId: String): Flow<List<Room>> =
        dao.observeByProject(projectId).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    /** Proverava unos, pa tek onda upisuje sobu. Mere koje je korisnik upisao vode se kao USER_CONFIRMED. */
    suspend fun createRoom(
        projectId: String,
        input: RoomInput,
        nowMillis: Long = System.currentTimeMillis()
    ): CreateRoomResult {
        val validation = RoomValidator.validate(input, dao.getNamesByProject(projectId))
        return when (validation) {
            is RoomValidation.Invalid -> CreateRoomResult.Invalid(validation.errors)
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
                CreateRoomResult.Success(room)
            }
        }
    }

    suspend fun deleteRoom(id: String) = dao.deleteById(id)
}
