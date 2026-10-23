package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Floors
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomInput
import com.zoran.aihomedesigner.domain.RoomInputErrors
import com.zoran.aihomedesigner.domain.RoomPlacement
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

    /**
     * Proverava unos, pa tek onda upisuje sobu. Mere koje je korisnik upisao vode se kao USER_CONFIRMED.
     * Nova soba dobija početni položaj na planu (desno od postojećih soba).
     */
    suspend fun createRoom(
        projectId: String,
        input: RoomInput,
        nowMillis: Long = System.currentTimeMillis(),
        floorLevel: Int = 0
    ): RoomSaveResult {
        val level = floorLevel.coerceIn(Floors.MIN_LEVEL, Floors.MAX_LEVEL)
        val sameFloor = dao.getByProject(projectId)
            .mapNotNull { it.toDomainOrNull() }
            .filter { it.floorLevel == level }

        // Ime sobe mora da bude jedinstveno samo na istom spratu (npr. "Kupatilo" može i u prizemlju i na spratu)
        val validation = RoomValidator.validate(input, sameFloor.map { it.name })
        return when (validation) {
            is RoomValidation.Invalid -> RoomSaveResult.Invalid(validation.errors)
            is RoomValidation.Valid -> {
                // Položaj se računa samo u odnosu na sobe istog sprata
                val existing = sameFloor
                val position = RoomPlacement.nextPosition(existing)
                val room = Room(
                    id = Room.newId(),
                    projectId = projectId,
                    name = validation.name,
                    lengthM = validation.lengthM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    wallThicknessM = validation.wallThicknessM,
                    dimensionSource = DimensionSource.USER_CONFIRMED,
                    createdAtMillis = nowMillis,
                    xM = position.xM,
                    yM = position.yM,
                    floorLevel = level
                )
                dao.insert(room.toEntity())
                RoomSaveResult.Success(room)
            }
        }
    }

    /**
     * Menja ime i mere postojeće sobe. Mere se menjaju samo onim što je korisnik upisao.
     * Ako se mere nisu promenile, izvor mera ostaje isti. Ako jesu, vode se kao USER_CONFIRMED.
     * Položaj sobe na planu se ne menja.
     */
    suspend fun updateRoom(roomId: String, input: RoomInput): RoomSaveResult {
        val existing = dao.getById(roomId)?.toDomainOrNull() ?: return RoomSaveResult.NotFound
        // Imena se porede samo sa drugim sobama istog sprata
        val otherNames = dao.getByProject(existing.projectId)
            .mapNotNull { it.toDomainOrNull() }
            .filter { it.id != roomId && it.floorLevel == existing.floorLevel }
            .map { it.name }

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

    /** Pomera sobu na planu. Neispravne koordinate (NaN, beskonačno, preko 1 km) se odbacuju. */
    suspend fun moveRoom(roomId: String, xM: Double, yM: Double) {
        if (!RoomPlacement.isValidCoordinate(xM) || !RoomPlacement.isValidCoordinate(yM)) return
        dao.updatePosition(roomId, xM, yM)
    }

    /** Daje položaj sobama koje ga još nemaju (npr. sobe napravljene pre uvođenja plana). */
    suspend fun placeUnplacedRooms(projectId: String) {
        val rooms = dao.getByProject(projectId).mapNotNull { it.toDomainOrNull() }
        for (placed in RoomPlacement.autoPlaceByFloor(rooms)) {
            dao.updatePosition(placed.id, placed.xM!!, placed.yM!!)
        }
    }

    suspend fun deleteRoom(id: String) = dao.deleteById(id)
}
