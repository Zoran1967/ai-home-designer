package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.RoomPlacement
import com.zoran.aihomedesigner.domain.Stairs
import com.zoran.aihomedesigner.domain.StairsInput
import com.zoran.aihomedesigner.domain.StairsInputErrors
import com.zoran.aihomedesigner.domain.StairsValidation
import com.zoran.aihomedesigner.domain.StairsValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface StairsSaveResult {
    data class Success(val stairs: Stairs) : StairsSaveResult
    data class Invalid(val errors: StairsInputErrors) : StairsSaveResult

    /** Stepenice koje se menjaju više ne postoje (npr. obrisane su). */
    data object NotFound : StairsSaveResult
}

class StairsRepository(private val dao: StairsDao) {

    fun observeByProject(projectId: String): Flow<List<Stairs>> =
        dao.observeByProject(projectId).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    /**
     * Proverava unos, pa tek onda upisuje stepenice. Mere koje je korisnik upisao vode se kao USER_CONFIRMED.
     * Položaj (xM, yM) je u koordinatama plana. Neispravna koordinata se zamenjuje nulom.
     * Projekat mora da postoji, inače baza odbija upis (spoljni ključ).
     */
    suspend fun createStairs(
        projectId: String,
        input: StairsInput,
        xM: Double,
        yM: Double,
        nowMillis: Long = System.currentTimeMillis()
    ): StairsSaveResult = when (val validation = StairsValidator.validate(input)) {
        is StairsValidation.Invalid -> StairsSaveResult.Invalid(validation.errors)
        is StairsValidation.Valid -> {
            val stairs = Stairs(
                id = Stairs.newId(),
                projectId = projectId,
                lowerLevel = validation.lowerLevel,
                xM = if (RoomPlacement.isValidCoordinate(xM)) xM else 0.0,
                yM = if (RoomPlacement.isValidCoordinate(yM)) yM else 0.0,
                lengthM = validation.lengthM,
                widthM = validation.widthM,
                directionDeg = validation.directionDeg,
                dimensionSource = DimensionSource.USER_CONFIRMED,
                createdAtMillis = nowMillis
            )
            dao.insert(stairs.toEntity())
            StairsSaveResult.Success(stairs)
        }
    }

    /** Menja sprat, mere i pravac. Položaj na planu se ne menja. */
    suspend fun updateStairs(stairsId: String, input: StairsInput): StairsSaveResult {
        val existing = dao.getById(stairsId)?.toDomainOrNull() ?: return StairsSaveResult.NotFound
        return when (val validation = StairsValidator.validate(input)) {
            is StairsValidation.Invalid -> StairsSaveResult.Invalid(validation.errors)
            is StairsValidation.Valid -> {
                val updated = existing.copy(
                    lowerLevel = validation.lowerLevel,
                    lengthM = validation.lengthM,
                    widthM = validation.widthM,
                    directionDeg = validation.directionDeg,
                    dimensionSource = DimensionSource.USER_CONFIRMED
                )
                dao.update(updated.toEntity())
                StairsSaveResult.Success(updated)
            }
        }
    }

    /** Pomera stepenice na planu. Neispravne koordinate se odbacuju. */
    suspend fun moveStairs(stairsId: String, xM: Double, yM: Double) {
        if (!RoomPlacement.isValidCoordinate(xM) || !RoomPlacement.isValidCoordinate(yM)) return
        dao.updatePosition(stairsId, xM, yM)
    }

    suspend fun deleteStairs(stairsId: String) = dao.deleteById(stairsId)
}
