package com.zoran.aihomedesigner.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
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
import com.zoran.aihomedesigner.data.StairsRepository
import com.zoran.aihomedesigner.data.StairsSaveResult
import com.zoran.aihomedesigner.domain.FloorMoveResult
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureInput
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.PlanImage
import com.zoran.aihomedesigner.domain.OpeningInput
import com.zoran.aihomedesigner.domain.Project
import com.zoran.aihomedesigner.domain.PropertyType
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.domain.RoomGlueResult
import com.zoran.aihomedesigner.domain.RoomInput
import com.zoran.aihomedesigner.domain.Stairs
import com.zoran.aihomedesigner.domain.StairsInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.get(application)
    private val repository = ProjectRepository(database.projectDao())
    private val roomRepository = RoomRepository(database.roomDao())
    private val planImageRepository = PlanImageRepository(application, database.planImageDao())
    private val openingRepository = OpeningRepository(database.openingDao(), database.roomDao())
    private val stairsRepository = StairsRepository(database.stairsDao())
    private val furnitureRepository = FurnitureRepository(
        database.furnitureDao(),
        database.roomDao(),
        database.openingDao()
    )

    /** null znači da se projekti još učitavaju iz baze. */
    val projects: StateFlow<List<Project>?> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _saveFailed = MutableStateFlow(false)

    /** Postaje true kad upis promene u bazu ne uspe (npr. pun telefon). Ekran tada prikazuje poruku. */
    val saveFailed: StateFlow<Boolean> = _saveFailed.asStateFlow()

    fun dismissSaveFailed() {
        _saveFailed.value = false
    }

    /**
     * Pokreće upis u bazu. Ako upis ne uspe, aplikacija se ne zatvara, nego se javlja [saveFailed].
     * Važi za brisanja i pomeranja.
     */
    private fun launchWrite(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ProjectsViewModel", "Upis u bazu nije uspeo", e)
                _saveFailed.value = true
            }
        }
    }

    /**
     * Upis iz forme (soba, vrata, nameštaj, stepenice). Ako upis ne uspe, javlja se [saveFailed],
     * a formi se vraća [fallback] (rezultat "ne postoji") da se zatvori i ne ostane zaključana.
     * Poruka na ekranu jasno kaže da promena nije sačuvana.
     */
    private fun <T> launchSave(onResult: (T) -> Unit, fallback: T, block: suspend () -> T) {
        viewModelScope.launch {
            val result = try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("ProjectsViewModel", "Upis iz forme nije uspeo", e)
                _saveFailed.value = true
                fallback
            }
            onResult(result)
        }
    }

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
        launchWrite {
            planImageRepository.deleteImage(id)
            repository.deleteProject(id)
        }
    }

    fun observeRooms(projectId: String): Flow<List<Room>> =
        roomRepository.observeRooms(projectId)

    fun createRoom(
        projectId: String,
        input: RoomInput,
        onResult: (RoomSaveResult) -> Unit,
        floorLevel: Int = 0
    ) {
        launchSave<RoomSaveResult>(onResult, RoomSaveResult.NotFound) {
            roomRepository.createRoom(projectId, input, floorLevel = floorLevel)
        }
    }

    fun updateRoom(
        roomId: String,
        input: RoomInput,
        onResult: (RoomSaveResult) -> Unit
    ) {
        launchSave<RoomSaveResult>(onResult, RoomSaveResult.NotFound) {
            roomRepository.updateRoom(roomId, input)
        }
    }

    fun moveRoom(roomId: String, xM: Double, yM: Double) {
        // Zalepljene sobe se pomeraju zajedno, a zaključana soba se ne pomera
        launchWrite {
            roomRepository.moveRoomGroup(roomId, xM, yM)
        }
    }

    fun setRoomLocked(roomId: String, locked: Boolean) {
        launchWrite {
            roomRepository.setLocked(roomId, locked)
        }
    }

    fun glueRoom(roomId: String, onResult: (RoomGlueResult) -> Unit) {
        launchWrite {
            onResult(roomRepository.glueWithNeighbours(roomId))
        }
    }

    fun unglueRoom(roomId: String) {
        launchWrite {
            roomRepository.unglue(roomId)
        }
    }

    fun moveRoomToFloor(roomId: String, targetLevel: Int, onResult: (FloorMoveResult) -> Unit) {
        launchWrite {
            onResult(roomRepository.moveRoomToFloor(roomId, targetLevel))
        }
    }

    fun placeUnplacedRooms(projectId: String) {
        launchWrite {
            roomRepository.placeUnplacedRooms(projectId)
        }
    }

    fun deleteRoom(id: String) {
        launchWrite {
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
        launchSave<OpeningSaveResult>(onResult, OpeningSaveResult.NotFound) {
            openingRepository.createOpening(roomId, input)
        }
    }

    fun updateOpening(
        openingId: String,
        input: OpeningInput,
        onResult: (OpeningSaveResult) -> Unit
    ) {
        launchSave<OpeningSaveResult>(onResult, OpeningSaveResult.NotFound) {
            openingRepository.updateOpening(openingId, input)
        }
    }

    fun deleteOpening(id: String) {
        launchWrite {
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
        launchSave<FurnitureSaveResult>(onResult, FurnitureSaveResult.NotFound) {
            furnitureRepository.createFurniture(roomId, input)
        }
    }

    fun updateFurniture(
        furnitureId: String,
        input: FurnitureInput,
        onResult: (FurnitureSaveResult) -> Unit
    ) {
        launchSave<FurnitureSaveResult>(onResult, FurnitureSaveResult.NotFound) {
            furnitureRepository.updateFurniture(furnitureId, input)
        }
    }

    fun moveFurniture(furnitureId: String, xM: Double, yM: Double, rotationDeg: Int) {
        launchWrite {
            furnitureRepository.moveFurniture(furnitureId, xM, yM, rotationDeg)
        }
    }

    fun deleteFurniture(id: String) {
        launchWrite {
            furnitureRepository.deleteFurniture(id)
        }
    }

    fun observeStairs(projectId: String): Flow<List<Stairs>> =
        stairsRepository.observeByProject(projectId)

    fun createStairs(
        projectId: String,
        input: StairsInput,
        xM: Double,
        yM: Double,
        onResult: (StairsSaveResult) -> Unit
    ) {
        launchSave<StairsSaveResult>(onResult, StairsSaveResult.NotFound) {
            stairsRepository.createStairs(projectId, input, xM, yM)
        }
    }

    fun updateStairs(stairsId: String, input: StairsInput, onResult: (StairsSaveResult) -> Unit) {
        launchSave<StairsSaveResult>(onResult, StairsSaveResult.NotFound) {
            stairsRepository.updateStairs(stairsId, input)
        }
    }

    fun moveStairs(stairsId: String, xM: Double, yM: Double) {
        launchWrite {
            stairsRepository.moveStairs(stairsId, xM, yM)
        }
    }

    fun deleteStairs(stairsId: String) {
        launchWrite {
            stairsRepository.deleteStairs(stairsId)
        }
    }

    fun observePlanImage(projectId: String): Flow<PlanImage?> =
        planImageRepository.observe(projectId)

    /** Kopira izabranu sliku u aplikaciju. Rezultat je null ako se slika ne može koristiti. */
    fun importPlanImage(
        projectId: String,
        uri: Uri,
        pageNumber: Int = 1,
        onResult: (ImportedImage?) -> Unit
    ) {
        viewModelScope.launch {
            onResult(planImageRepository.copyImage(projectId, uri, pageNumber))
        }
    }

    /** Broj strana PDF-a, ili null ako se PDF ne može otvoriti. */
    fun pdfPageCount(uri: Uri, onResult: (Int?) -> Unit) {
        viewModelScope.launch {
            onResult(planImageRepository.pdfPageCount(uri))
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
        launchWrite {
            planImageRepository.discard(image)
        }
    }

    fun movePlanImage(projectId: String, xM: Double, yM: Double) {
        launchWrite {
            planImageRepository.moveImage(projectId, xM, yM)
        }
    }

    fun deletePlanImage(projectId: String) {
        launchWrite {
            planImageRepository.deleteImage(projectId)
        }
    }

    suspend fun loadPlanBitmap(fileName: String, maxSidePx: Int): Bitmap? =
        planImageRepository.loadBitmap(fileName, maxSidePx)
}
