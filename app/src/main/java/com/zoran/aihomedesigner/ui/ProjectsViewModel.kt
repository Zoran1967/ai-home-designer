package com.zoran.aihomedesigner.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zoran.aihomedesigner.data.AppDatabase
import com.zoran.aihomedesigner.data.CreateProjectResult
import com.zoran.aihomedesigner.data.FurnitureRepository
import com.zoran.aihomedesigner.data.ImportedImage
import com.zoran.aihomedesigner.data.PlanImageRepository
import com.zoran.aihomedesigner.data.FurnitureSaveResult
import com.zoran.aihomedesigner.data.OpeningRepository
import com.zoran.aihomedesigner.data.OpeningSaveResult
import com.zoran.aihomedesigner.data.ProjectRepository
import com.zoran.aihomedesigner.data.RoomRepository
import com.zoran.aihomedesigner.data.RoomSaveResult
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureInput
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.PlanImage
import com.zoran.aihomedesigner.domain.OpeningInput
import com.zoran.aihomedesigner.domain.Project
import com.zoran.aihomedesigner.domain.PropertyType
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomInput
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.get(application)
    private val repository = ProjectRepository(database.projectDao())
    private val roomRepository = RoomRepository(database.roomDao())
    private val planImageRepository = PlanImageRepository(application, database.planImageDao())
    private val openingRepository = OpeningRepository(database.openingDao(), database.roomDao())
    private val furnitureRepository = FurnitureRepository(
        database.furnitureDao(),
        database.roomDao(),
        database.openingDao()
    )

    /** null znači da se projekti još učitavaju iz baze. */
    val projects: StateFlow<List<Project>?> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun createProject(
        rawName: String,
        type: PropertyType,
        onResult: (CreateProjectResult) -> Unit
    ) {
        viewModelScope.launch {
            onResult(repository.createProject(rawName, type))
        }
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            planImageRepository.deleteImage(id)
            repository.deleteProject(id)
        }
    }

    fun observeRooms(projectId: String): Flow<List<Room>> =
        roomRepository.observeRooms(projectId)

    fun createRoom(
        projectId: String,
        input: RoomInput,
        onResult: (RoomSaveResult) -> Unit
    ) {
        viewModelScope.launch {
            onResult(roomRepository.createRoom(projectId, input))
        }
    }

    fun updateRoom(
        roomId: String,
        input: RoomInput,
        onResult: (RoomSaveResult) -> Unit
    ) {
        viewModelScope.launch {
            onResult(roomRepository.updateRoom(roomId, input))
        }
    }

    fun moveRoom(roomId: String, xM: Double, yM: Double) {
        viewModelScope.launch {
            roomRepository.moveRoom(roomId, xM, yM)
        }
    }

    fun placeUnplacedRooms(projectId: String) {
        viewModelScope.launch {
            roomRepository.placeUnplacedRooms(projectId)
        }
    }

    fun deleteRoom(id: String) {
        viewModelScope.launch {
            roomRepository.deleteRoom(id)
        }
    }

    fun observeOpenings(roomId: String): Flow<List<Opening>> =
        openingRepository.observeByRoom(roomId)

    fun observeProjectOpenings(projectId: String): Flow<List<Opening>> =
        openingRepository.observeByProject(projectId)

    fun createOpening(
        roomId: String,
        input: OpeningInput,
        onResult: (OpeningSaveResult) -> Unit
    ) {
        viewModelScope.launch {
            onResult(openingRepository.createOpening(roomId, input))
        }
    }

    fun updateOpening(
        openingId: String,
        input: OpeningInput,
        onResult: (OpeningSaveResult) -> Unit
    ) {
        viewModelScope.launch {
            onResult(openingRepository.updateOpening(openingId, input))
        }
    }

    fun deleteOpening(id: String) {
        viewModelScope.launch {
            openingRepository.deleteOpening(id)
        }
    }

    fun observeFurniture(roomId: String): Flow<List<Furniture>> =
        furnitureRepository.observeByRoom(roomId)

    fun observeProjectFurniture(projectId: String): Flow<List<Furniture>> =
        furnitureRepository.observeByProject(projectId)

    fun createFurniture(
        roomId: String,
        input: FurnitureInput,
        onResult: (FurnitureSaveResult) -> Unit
    ) {
        viewModelScope.launch {
            onResult(furnitureRepository.createFurniture(roomId, input))
        }
    }

    fun updateFurniture(
        furnitureId: String,
        input: FurnitureInput,
        onResult: (FurnitureSaveResult) -> Unit
    ) {
        viewModelScope.launch {
            onResult(furnitureRepository.updateFurniture(furnitureId, input))
        }
    }

    fun moveFurniture(furnitureId: String, xM: Double, yM: Double, rotationDeg: Int) {
        viewModelScope.launch {
            furnitureRepository.moveFurniture(furnitureId, xM, yM, rotationDeg)
        }
    }

    fun deleteFurniture(id: String) {
        viewModelScope.launch {
            furnitureRepository.deleteFurniture(id)
        }
    }

    fun observePlanImage(projectId: String): Flow<PlanImage?> =
        planImageRepository.observe(projectId)

    /** Kopira izabranu sliku u aplikaciju. Rezultat je null ako se slika ne može koristiti. */
    fun importPlanImage(projectId: String, uri: Uri, onResult: (ImportedImage?) -> Unit) {
        viewModelScope.launch {
            onResult(planImageRepository.copyImage(projectId, uri))
        }
    }

    fun savePlanCalibration(
        projectId: String,
        image: ImportedImage,
        metersPerPixel: Double,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            planImageRepository.saveCalibrated(projectId, image, metersPerPixel)
            onDone()
        }
    }

    fun discardImportedImage(image: ImportedImage) {
        viewModelScope.launch {
            planImageRepository.discard(image)
        }
    }

    fun movePlanImage(projectId: String, xM: Double, yM: Double) {
        viewModelScope.launch {
            planImageRepository.moveImage(projectId, xM, yM)
        }
    }

    fun deletePlanImage(projectId: String) {
        viewModelScope.launch {
            planImageRepository.deleteImage(projectId)
        }
    }

    suspend fun loadPlanBitmap(fileName: String, maxSidePx: Int): Bitmap? =
        planImageRepository.loadBitmap(fileName, maxSidePx)
}
