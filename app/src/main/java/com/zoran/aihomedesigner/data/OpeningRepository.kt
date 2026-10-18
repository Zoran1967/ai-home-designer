package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.DimensionSource
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.OpeningInput
import com.zoran.aihomedesigner.domain.OpeningInputErrors
import com.zoran.aihomedesigner.domain.OpeningValidation
import com.zoran.aihomedesigner.domain.OpeningValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface OpeningSaveResult {
    data class Success(val opening: Opening) : OpeningSaveResult
    data class Invalid(val errors: OpeningInputErrors) : OpeningSaveResult

    /** Otvor ili soba kojoj pripada više ne postoji. */
    data object NotFound : OpeningSaveResult
}

class OpeningRepository(
    private val openingDao: OpeningDao,
    private val roomDao: RoomDao
) {

    fun observeOpenings(projectId: String): Flow<List<Opening>> =
        openingDao.observeByProject(projectId)
            .map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    /** Proverava otvor (staje li u zid i u visinu sobe, preklapanje), pa tek onda upisuje. */
    suspend fun createOpening(
        roomId: String,
        input: OpeningInput,
        nowMillis: Long = System.currentTimeMillis()
    ): OpeningSaveResult {
        val room = roomDao.getById(roomId)?.toDomainOrNull() ?: return OpeningSaveResult.NotFound
        val others = openingDao.getByRoom(roomId).mapNotNull { it.toDomainOrNull() }

        return when (val validation = OpeningValidator.validate(input, room, others)) {
            is OpeningValidation.Invalid -> OpeningSaveResult.Invalid(validation.errors)
            is OpeningValidation.Valid -> {
                val opening = Opening(
                    id = Opening.newId(),
                    roomId = roomId,
                    side = validation.side,
                    type = validation.type,
                    offsetM = validation.offsetM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    sillHeightM = validation.sillHeightM,
                    swing = validation.swing,
                    hinge = validation.hinge,
                    dimensionSource = DimensionSource.USER_CONFIRMED,
                    createdAtMillis = nowMillis
                )
                openingDao.insert(opening.toEntity())
                OpeningSaveResult.Success(opening)
            }
        }
    }

    /** Menja postojeći otvor. Ako se ništa nije promenilo, izvor mera ostaje isti. */
    suspend fun updateOpening(openingId: String, input: OpeningInput): OpeningSaveResult {
        val existing = openingDao.getById(openingId)?.toDomainOrNull()
            ?: return OpeningSaveResult.NotFound
        val room = roomDao.getById(existing.roomId)?.toDomainOrNull()
            ?: return OpeningSaveResult.NotFound
        val others = openingDao.getByRoom(existing.roomId)
            .mapNotNull { it.toDomainOrNull() }
            .filter { it.id != openingId }

        return when (val validation = OpeningValidator.validate(input, room, others)) {
            is OpeningValidation.Invalid -> OpeningSaveResult.Invalid(validation.errors)
            is OpeningValidation.Valid -> {
                val changed = validation.side != existing.side ||
                    validation.type != existing.type ||
                    validation.offsetM != existing.offsetM ||
                    validation.widthM != existing.widthM ||
                    validation.heightM != existing.heightM ||
                    validation.sillHeightM != existing.sillHeightM ||
                    validation.swing != existing.swing ||
                    validation.hinge != existing.hinge
                val updated = existing.copy(
                    side = validation.side,
                    type = validation.type,
                    offsetM = validation.offsetM,
                    widthM = validation.widthM,
                    heightM = validation.heightM,
                    sillHeightM = validation.sillHeightM,
                    swing = validation.swing,
                    hinge = validation.hinge,
                    dimensionSource = if (changed) {
                        DimensionSource.USER_CONFIRMED
                    } else {
                        existing.dimensionSource
                    }
                )
                openingDao.update(updated.toEntity())
                OpeningSaveResult.Success(updated)
            }
        }
    }

    suspend fun deleteOpening(id: String) = openingDao.deleteById(id)
}
