package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.OpeningInput
import com.zoran.aihomedesigner.domain.OpeningInputErrors
import com.zoran.aihomedesigner.domain.OpeningType
import com.zoran.aihomedesigner.domain.OpeningValidation
import com.zoran.aihomedesigner.domain.OpeningValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface OpeningSaveResult {
    data class Success(val opening: Opening) : OpeningSaveResult
    data class Invalid(val errors: OpeningInputErrors) : OpeningSaveResult

    /** Soba ili otvor koji se menja više ne postoji (npr. obrisan je). */
    data object NotFound : OpeningSaveResult
}

class OpeningRepository(
    private val dao: OpeningDao,
    private val roomDao: RoomDao
) {

    fun observeByRoom(roomId: String): Flow<List<Opening>> =
        dao.observeByRoom(roomId).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    fun observeByProject(projectId: String): Flow<List<Opening>> =
        dao.observeByProject(projectId).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    /** Proverava unos, pa tek onda upisuje otvor. Mere koje je korisnik upisao vode se kao USER_CONFIRMED. */
    suspend fun createOpening(
        roomId: String,
        input: OpeningInput,
        nowMillis: Long = System.currentTimeMillis()
    ): OpeningSaveResult {
        val room = roomDao.getById(roomId)?.toDomainOrNull() ?: return OpeningSaveResult.NotFound
        val existing = dao.getByRoom(roomId).mapNotNull { it.toDomainOrNull() }

        return when (val validation = OpeningValidator.validate(input, room, existing)) {
            is OpeningValidation.Invalid -> OpeningSaveResult.Invalid(validation.errors)
            is OpeningValidation.Valid -> {
                val opening = Opening(
                    id = Opening.newId(),
                    roomId = roomId,
                    type = input.type,
                    side = input.side,
                    offsetM = validation.offsetM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    sillHeightM = validation.sillHeightM,
                    swing = validation.swing,
                    hinge = validation.hinge,
                    dimensionSource = DimensionSource.USER_CONFIRMED,
                    createdAtMillis = nowMillis
                )
                dao.insert(opening.toEntity())
                OpeningSaveResult.Success(opening)
            }
        }
    }

    suspend fun updateOpening(openingId: String, input: OpeningInput): OpeningSaveResult {
        val existing = dao.getById(openingId)?.toDomainOrNull() ?: return OpeningSaveResult.NotFound
        val room = roomDao.getById(existing.roomId)?.toDomainOrNull() ?: return OpeningSaveResult.NotFound
        val others = dao.getByRoom(existing.roomId).mapNotNull { it.toDomainOrNull() }

        return when (val validation = OpeningValidator.validate(input, room, others, editingId = openingId)) {
            is OpeningValidation.Invalid -> OpeningSaveResult.Invalid(validation.errors)
            is OpeningValidation.Valid -> {
                val changed =
                    input.type != existing.type ||
                        input.side != existing.side ||
                        validation.offsetM != existing.offsetM ||
                        validation.widthM != existing.widthM ||
                        validation.heightM != existing.heightM ||
                        validation.sillHeightM != existing.sillHeightM
                val updated = existing.copy(
                    type = input.type,
                    side = input.side,
                    offsetM = validation.offsetM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    sillHeightM = validation.sillHeightM,
                    swing = validation.swing,
                    hinge = validation.hinge,
                    dimensionSource = if (changed) DimensionSource.USER_CONFIRMED else existing.dimensionSource
                )
                dao.update(updated.toEntity())
                OpeningSaveResult.Success(updated)
            }
        }
    }

    suspend fun deleteOpening(id: String) = dao.deleteById(id)
}
