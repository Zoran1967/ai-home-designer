package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureChecker
import com.zoran.aihomedesigner.domain.FurnitureInput
import com.zoran.aihomedesigner.domain.FurnitureInputErrors
import com.zoran.aihomedesigner.domain.FurnitureValidation
import com.zoran.aihomedesigner.domain.FurnitureValidator
import com.zoran.aihomedesigner.domain.RoomPlacement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface FurnitureSaveResult {
    data class Success(val furniture: Furniture) : FurnitureSaveResult
    data class Invalid(val errors: FurnitureInputErrors) : FurnitureSaveResult

    /** Soba ili predmet koji se menja više ne postoji (npr. obrisan je). */
    data object NotFound : FurnitureSaveResult
}

class FurnitureRepository(
    private val dao: FurnitureDao,
    private val roomDao: RoomDao,
    private val openingDao: OpeningDao
) {

    fun observeByRoom(roomId: String): Flow<List<Furniture>> =
        dao.observeByRoom(roomId).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    fun observeByProject(projectId: String): Flow<List<Furniture>> =
        dao.observeByProject(projectId).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    /**
     * Proverava unos, pa upisuje predmet. Predmet dobija prvo slobodno mesto u sobi
     * (bez preklapanja i bez smetanja vratima). Ako ga nema, postavlja se u ugao sobe,
     * a provera nameštaja će prijaviti problem. Mere koje je korisnik upisao vode se kao USER_CONFIRMED.
     */
    suspend fun createFurniture(
        roomId: String,
        input: FurnitureInput,
        nowMillis: Long = System.currentTimeMillis()
    ): FurnitureSaveResult {
        val room = roomDao.getById(roomId)?.toDomainOrNull() ?: return FurnitureSaveResult.NotFound

        return when (val validation = FurnitureValidator.validate(input, room)) {
            is FurnitureValidation.Invalid -> FurnitureSaveResult.Invalid(validation.errors)
            is FurnitureValidation.Valid -> {
                val others = dao.getByRoom(roomId).mapNotNull { it.toDomainOrNull() }
                val openings = openingDao.getByRoom(roomId).mapNotNull { it.toDomainOrNull() }

                // Ako predmet staje u sobu samo okrenut, odmah se okreće za 90 stepeni.
                val eps = com.zoran.aihomedesigner.domain.RoomGeometry.EPSILON_M
                val fitsAsIs = validation.lengthM <= room.lengthM + eps && validation.widthM <= room.widthM + eps
                val turns = if (fitsAsIs) 0 else 1
                val footprintLength = if (turns == 0) validation.lengthM else validation.widthM
                val footprintWidth = if (turns == 0) validation.widthM else validation.lengthM

                val spot = FurnitureChecker.findFreeSpot(room, others, openings, footprintLength, footprintWidth)
                val furniture = Furniture(
                    id = Furniture.newId(),
                    roomId = roomId,
                    name = validation.name,
                    category = validation.category,
                    lengthM = validation.lengthM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    xM = spot?.xM ?: 0.0,
                    yM = spot?.yM ?: 0.0,
                    quarterTurns = turns,
                    dimensionSource = DimensionSource.USER_CONFIRMED,
                    createdAtMillis = nowMillis
                )
                dao.insert(furniture.toEntity())
                FurnitureSaveResult.Success(furniture)
            }
        }
    }

    /** Menja ime, vrstu i mere predmeta. Položaj i okretanje ostaju isti. */
    suspend fun updateFurniture(furnitureId: String, input: FurnitureInput): FurnitureSaveResult {
        val existing = dao.getById(furnitureId)?.toDomainOrNull() ?: return FurnitureSaveResult.NotFound
        val room = roomDao.getById(existing.roomId)?.toDomainOrNull() ?: return FurnitureSaveResult.NotFound

        return when (val validation = FurnitureValidator.validate(input, room)) {
            is FurnitureValidation.Invalid -> FurnitureSaveResult.Invalid(validation.errors)
            is FurnitureValidation.Valid -> {
                val dimensionsChanged =
                    validation.lengthM != existing.lengthM ||
                        validation.widthM != existing.widthM ||
                        validation.heightM != existing.heightM
                val updated = existing.copy(
                    name = validation.name,
                    category = validation.category,
                    lengthM = validation.lengthM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    dimensionSource = if (dimensionsChanged) {
                        DimensionSource.USER_CONFIRMED
                    } else {
                        existing.dimensionSource
                    }
                )
                dao.update(updated.toEntity())
                FurnitureSaveResult.Success(updated)
            }
        }
    }

    /** Pomera predmet u sobi. Neispravne koordinate (NaN, beskonačno, preko 1 km) se odbacuju. */
    suspend fun moveFurniture(furnitureId: String, xM: Double, yM: Double) {
        if (!RoomPlacement.isValidCoordinate(xM) || !RoomPlacement.isValidCoordinate(yM)) return
        dao.updatePosition(furnitureId, xM, yM)
    }

    /** Okreće predmet za 90 stepeni u smeru kazaljke na satu. */
    suspend fun rotateFurniture(furnitureId: String) {
        val existing = dao.getById(furnitureId)?.toDomainOrNull() ?: return
        dao.updateQuarterTurns(furnitureId, (existing.quarterTurns + 1) % 4)
    }

    suspend fun deleteFurniture(id: String) = dao.deleteById(id)
}
