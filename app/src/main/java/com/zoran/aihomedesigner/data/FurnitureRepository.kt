package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureInput
import com.zoran.aihomedesigner.domain.FurnitureInputErrors
import com.zoran.aihomedesigner.domain.FurniturePlacement
import com.zoran.aihomedesigner.domain.FurnitureValidation
import com.zoran.aihomedesigner.domain.FurnitureValidator
import com.zoran.aihomedesigner.domain.PlanPoint
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
     * Proverava unos, pa tek onda upisuje predmet. Mere koje je korisnik upisao vode se kao USER_CONFIRMED.
     * Novi predmet dobija prvo slobodno mesto u sobi (ili gornji levi ugao ako mesta nema).
     */
    suspend fun createFurniture(
        roomId: String,
        input: FurnitureInput,
        nowMillis: Long = System.currentTimeMillis()
    ): FurnitureSaveResult {
        val room = roomDao.getById(roomId)?.toDomainOrNull() ?: return FurnitureSaveResult.NotFound

        return when (val validation = FurnitureValidator.validate(input)) {
            is FurnitureValidation.Invalid -> FurnitureSaveResult.Invalid(validation.errors)
            is FurnitureValidation.Valid -> {
                val existing = dao.getByRoom(roomId).mapNotNull { it.toDomainOrNull() }
                val openings = openingDao.getByRoom(roomId).mapNotNull { it.toDomainOrNull() }
                val turned = validation.rotationDeg % 180 != 0
                val footprintLength = if (turned) validation.widthM else validation.lengthM
                val footprintWidth = if (turned) validation.lengthM else validation.widthM
                val spot = FurniturePlacement.firstFreeSpot(
                    room, existing, footprintLength, footprintWidth, openings
                ) ?: PlanPoint(0.0, 0.0)

                val furniture = Furniture(
                    id = Furniture.newId(),
                    roomId = roomId,
                    name = validation.name,
                    category = input.category,
                    lengthM = validation.lengthM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    xM = spot.xM,
                    yM = spot.yM,
                    rotationDeg = validation.rotationDeg,
                    dimensionSource = DimensionSource.USER_CONFIRMED,
                    createdAtMillis = nowMillis
                )
                dao.insert(furniture.toEntity())
                FurnitureSaveResult.Success(furniture)
            }
        }
    }

    /**
     * Menja ime, kategoriju, mere i okretanje. Položaj u sobi se ne menja (nova mera može da izvuče
     * predmet iz sobe, ali to prijavljuje provera rasporeda, a ne ova funkcija).
     */
    suspend fun updateFurniture(furnitureId: String, input: FurnitureInput): FurnitureSaveResult {
        val existing = dao.getById(furnitureId)?.toDomainOrNull() ?: return FurnitureSaveResult.NotFound

        return when (val validation = FurnitureValidator.validate(input)) {
            is FurnitureValidation.Invalid -> FurnitureSaveResult.Invalid(validation.errors)
            is FurnitureValidation.Valid -> {
                val dimensionsChanged =
                    validation.lengthM != existing.lengthM ||
                        validation.widthM != existing.widthM ||
                        validation.heightM != existing.heightM
                val updated = existing.copy(
                    name = validation.name,
                    category = input.category,
                    lengthM = validation.lengthM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    rotationDeg = validation.rotationDeg,
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

    /** Pomera i okreće predmet u sobi. Neispravne koordinate ili ugao se odbacuju. */
    suspend fun moveFurniture(furnitureId: String, xM: Double, yM: Double, rotationDeg: Int) {
        if (!RoomPlacement.isValidCoordinate(xM) || !RoomPlacement.isValidCoordinate(yM)) return
        val rotation = FurnitureValidator.normalizeRotation(rotationDeg) ?: return
        dao.updatePlacement(furnitureId, xM, yM, rotation)
    }

    suspend fun deleteFurniture(id: String) = dao.deleteById(id)
}
