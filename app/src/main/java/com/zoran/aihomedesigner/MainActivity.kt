package com.zoran.aihomedesigner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoran.aihomedesigner.domain.Furniture
import com.zoran.aihomedesigner.domain.FurnitureLayoutValidator
import com.zoran.aihomedesigner.domain.Opening
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.ui.CrashScreen
import com.zoran.aihomedesigner.ui.CreateProjectScreen
import com.zoran.aihomedesigner.ui.FurnitureFormScreen
import com.zoran.aihomedesigner.ui.FurnitureListScreen
import com.zoran.aihomedesigner.ui.OpeningFormScreen
import com.zoran.aihomedesigner.ui.OpeningListScreen
import com.zoran.aihomedesigner.ui.PlanScreen
import com.zoran.aihomedesigner.ui.ProjectListScreen
import com.zoran.aihomedesigner.ui.ProjectsViewModel
import com.zoran.aihomedesigner.ui.RoomFormScreen
import com.zoran.aihomedesigner.ui.RoomListScreen
import kotlinx.coroutines.flow.flowOf

private enum class Screen {
    PROJECTS,
    CREATE_PROJECT,
    ROOMS,
    CREATE_ROOM,
    EDIT_ROOM,
    PLAN,
    OPENINGS,
    CREATE_OPENING,
    EDIT_OPENING,
    FURNITURE,
    CREATE_FURNITURE,
    EDIT_FURNITURE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Ako se aplikacija prošli put neočekivano zatvorila, prvo prikaži opis greške.
                    var crashReport by remember { mutableStateOf(CrashReporter.readPending(applicationContext)) }
                    val report = crashReport
                    if (report != null) {
                        CrashScreen(
                            report = report,
                            onContinue = {
                                CrashReporter.clear(applicationContext)
                                crashReport = null
                            }
                        )
                    } else {
                        AppRoot()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(viewModel: ProjectsViewModel = viewModel()) {
    var screen by rememberSaveable { mutableStateOf(Screen.PROJECTS) }
    var selectedProjectId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRoomId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedOpeningId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedFurnitureId by rememberSaveable { mutableStateOf<String?>(null) }

    val projects by viewModel.projects.collectAsState()
    val selectedProject = projects?.firstOrNull { it.id == selectedProjectId }

    val roomsFlow = remember(selectedProjectId) {
        selectedProjectId?.let { viewModel.observeRooms(it) } ?: flowOf<List<Room>?>(null)
    }
    val rooms by roomsFlow.collectAsState(initial = null)
    val selectedRoom = rooms?.firstOrNull { it.id == selectedRoomId }

    // Otvori izabrane sobe (za ekran vrata i prozora) i svi otvori projekta (za plan).
    val roomOpeningsFlow = remember(selectedRoomId) {
        selectedRoomId?.let { viewModel.observeOpenings(it) } ?: flowOf<List<Opening>?>(null)
    }
    val roomOpenings by roomOpeningsFlow.collectAsState(initial = null)
    val openingToEdit = roomOpenings?.firstOrNull { it.id == selectedOpeningId }

    val roomFurnitureFlow = remember(selectedRoomId) {
        selectedRoomId?.let { viewModel.observeFurniture(it) } ?: flowOf<List<Furniture>?>(null)
    }
    val roomFurniture by roomFurnitureFlow.collectAsState(initial = null)
    val furnitureToEdit = roomFurniture?.firstOrNull { it.id == selectedFurnitureId }

    val projectOpeningsFlow = remember(selectedProjectId) {
        selectedProjectId?.let { viewModel.observeProjectOpenings(it) } ?: flowOf<List<Opening>?>(null)
    }
    val projectOpenings by projectOpeningsFlow.collectAsState(initial = null)

    val projectFurnitureFlow = remember(selectedProjectId) {
        selectedProjectId?.let { viewModel.observeProjectFurniture(it) } ?: flowOf<List<Furniture>?>(null)
    }
    val projectFurniture by projectFurnitureFlow.collectAsState(initial = null)

    BackHandler(enabled = screen != Screen.PROJECTS) {
        screen = when (screen) {
            Screen.CREATE_ROOM, Screen.EDIT_ROOM, Screen.PLAN, Screen.OPENINGS, Screen.FURNITURE -> Screen.ROOMS
            Screen.CREATE_OPENING, Screen.EDIT_OPENING -> Screen.OPENINGS
            Screen.CREATE_FURNITURE, Screen.EDIT_FURNITURE -> Screen.FURNITURE
            else -> {
                selectedProjectId = null
                Screen.PROJECTS
            }
        }
    }

    // Ako je izabrani projekat, soba ili otvor nestao (npr. obrisan), vrati se korak nazad.
    LaunchedEffect(
        projects, rooms, roomOpenings, roomFurniture,
        selectedProjectId, selectedRoomId, selectedOpeningId, selectedFurnitureId, screen
    ) {
        val needsProject = screen != Screen.PROJECTS && screen != Screen.CREATE_PROJECT
        val needsRoom = screen == Screen.EDIT_ROOM || screen == Screen.OPENINGS ||
            screen == Screen.CREATE_OPENING || screen == Screen.EDIT_OPENING ||
            screen == Screen.FURNITURE || screen == Screen.CREATE_FURNITURE || screen == Screen.EDIT_FURNITURE
        if (needsProject && projects != null && selectedProject == null) {
            selectedProjectId = null
            screen = Screen.PROJECTS
        } else if (needsRoom && rooms != null && selectedRoom == null) {
            screen = Screen.ROOMS
        } else if (screen == Screen.EDIT_OPENING && roomOpenings != null && openingToEdit == null) {
            screen = Screen.OPENINGS
        } else if (screen == Screen.EDIT_FURNITURE && roomFurniture != null && furnitureToEdit == null) {
            screen = Screen.FURNITURE
        }
    }

    when (screen) {
        Screen.PROJECTS -> ProjectListScreen(
            projects = projects,
            onNewProject = { screen = Screen.CREATE_PROJECT },
            onOpenProject = {
                selectedProjectId = it.id
                screen = Screen.ROOMS
            },
            onDeleteProject = { viewModel.deleteProject(it.id) }
        )

        Screen.CREATE_PROJECT -> CreateProjectScreen(
            onCreate = { name, type, onResult -> viewModel.createProject(name, type, onResult) },
            onDone = { screen = Screen.PROJECTS },
            onCancel = { screen = Screen.PROJECTS }
        )

        Screen.ROOMS -> selectedProject?.let { project ->
            RoomListScreen(
                projectName = project.name,
                rooms = rooms,
                onBack = {
                    selectedProjectId = null
                    screen = Screen.PROJECTS
                },
                onAddRoom = { screen = Screen.CREATE_ROOM },
                onOpenRoom = {
                    selectedRoomId = it.id
                    screen = Screen.EDIT_ROOM
                },
                onOpenPlan = { screen = Screen.PLAN },
                onOpenOpenings = {
                    selectedRoomId = it.id
                    screen = Screen.OPENINGS
                },
                onOpenFurniture = {
                    selectedRoomId = it.id
                    screen = Screen.FURNITURE
                },
                onDeleteRoom = { viewModel.deleteRoom(it.id) }
            )
        }

        Screen.CREATE_ROOM -> selectedProject?.let { project ->
            RoomFormScreen(
                initialRoom = null,
                onSubmit = { input, onResult -> viewModel.createRoom(project.id, input, onResult) },
                onDone = { screen = Screen.ROOMS },
                onCancel = { screen = Screen.ROOMS }
            )
        }

        Screen.EDIT_ROOM -> selectedRoom?.let { room ->
            RoomFormScreen(
                initialRoom = room,
                onSubmit = { input, onResult -> viewModel.updateRoom(room.id, input, onResult) },
                onDone = { screen = Screen.ROOMS },
                onCancel = { screen = Screen.ROOMS }
            )
        }

        Screen.PLAN -> selectedProject?.let { project ->
            PlanScreen(
                rooms = rooms,
                openings = projectOpenings.orEmpty(),
                furniture = projectFurniture.orEmpty(),
                onBack = { screen = Screen.ROOMS },
                onMoveRoom = { id, x, y -> viewModel.moveRoom(id, x, y) },
                onPlaceUnplaced = { viewModel.placeUnplacedRooms(project.id) },
                onMoveFurniture = { id, x, y, rotation -> viewModel.moveFurniture(id, x, y, rotation) }
            )
        }

        Screen.OPENINGS -> selectedRoom?.let { room ->
            OpeningListScreen(
                room = room,
                openings = roomOpenings,
                onBack = { screen = Screen.ROOMS },
                onAdd = { screen = Screen.CREATE_OPENING },
                onOpen = {
                    selectedOpeningId = it.id
                    screen = Screen.EDIT_OPENING
                },
                onDelete = { viewModel.deleteOpening(it.id) }
            )
        }

        Screen.CREATE_OPENING -> selectedRoom?.let { room ->
            OpeningFormScreen(
                room = room,
                initialOpening = null,
                onSubmit = { input, onResult -> viewModel.createOpening(room.id, input, onResult) },
                onDone = { screen = Screen.OPENINGS },
                onCancel = { screen = Screen.OPENINGS }
            )
        }

        Screen.EDIT_OPENING -> {
            val room = selectedRoom
            val opening = openingToEdit
            if (room != null && opening != null) {
                OpeningFormScreen(
                    room = room,
                    initialOpening = opening,
                    onSubmit = { input, onResult -> viewModel.updateOpening(opening.id, input, onResult) },
                    onDone = { screen = Screen.OPENINGS },
                    onCancel = { screen = Screen.OPENINGS }
                )
            }
        }

        Screen.FURNITURE -> selectedRoom?.let { room ->
            FurnitureListScreen(
                room = room,
                furniture = roomFurniture,
                issues = FurnitureLayoutValidator.validate(
                    room,
                    roomFurniture.orEmpty(),
                    roomOpenings.orEmpty()
                ),
                onBack = { screen = Screen.ROOMS },
                onAdd = { screen = Screen.CREATE_FURNITURE },
                onOpen = {
                    selectedFurnitureId = it.id
                    screen = Screen.EDIT_FURNITURE
                },
                onDelete = { viewModel.deleteFurniture(it.id) }
            )
        }

        Screen.CREATE_FURNITURE -> selectedRoom?.let { room ->
            FurnitureFormScreen(
                room = room,
                initialFurniture = null,
                onSubmit = { input, onResult -> viewModel.createFurniture(room.id, input, onResult) },
                onDone = { screen = Screen.FURNITURE },
                onCancel = { screen = Screen.FURNITURE }
            )
        }

        Screen.EDIT_FURNITURE -> {
            val room = selectedRoom
            val item = furnitureToEdit
            if (room != null && item != null) {
                FurnitureFormScreen(
                    room = room,
                    initialFurniture = item,
                    onSubmit = { input, onResult -> viewModel.updateFurniture(item.id, input, onResult) },
                    onDone = { screen = Screen.FURNITURE },
                    onCancel = { screen = Screen.FURNITURE }
                )
            }
        }
    }
}
