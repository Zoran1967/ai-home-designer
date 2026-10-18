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
import com.zoran.aihomedesigner.domain.Room
import com.zoran.aihomedesigner.ui.CreateProjectScreen
import com.zoran.aihomedesigner.ui.ProjectListScreen
import com.zoran.aihomedesigner.ui.ProjectsViewModel
import com.zoran.aihomedesigner.ui.RoomFormScreen
import com.zoran.aihomedesigner.ui.RoomListScreen
import kotlinx.coroutines.flow.flowOf

private enum class Screen { PROJECTS, CREATE_PROJECT, ROOMS, CREATE_ROOM, EDIT_ROOM }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
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

    val projects by viewModel.projects.collectAsState()
    val selectedProject = projects?.firstOrNull { it.id == selectedProjectId }

    val roomsFlow = remember(selectedProjectId) {
        selectedProjectId?.let { viewModel.observeRooms(it) } ?: flowOf<List<Room>?>(null)
    }
    val rooms by roomsFlow.collectAsState(initial = null)
    val roomToEdit = rooms?.firstOrNull { it.id == selectedRoomId }

    BackHandler(enabled = screen != Screen.PROJECTS) {
        screen = when (screen) {
            Screen.CREATE_ROOM, Screen.EDIT_ROOM -> Screen.ROOMS
            else -> {
                selectedProjectId = null
                Screen.PROJECTS
            }
        }
    }

    // Ako je izabrani projekat ili soba nestala (npr. obrisana), vrati se korak nazad.
    LaunchedEffect(projects, rooms, selectedProjectId, selectedRoomId, screen) {
        val needsProject = screen == Screen.ROOMS || screen == Screen.CREATE_ROOM ||
            screen == Screen.EDIT_ROOM
        if (needsProject && projects != null && selectedProject == null) {
            selectedProjectId = null
            screen = Screen.PROJECTS
        } else if (screen == Screen.EDIT_ROOM && rooms != null && roomToEdit == null) {
            screen = Screen.ROOMS
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

        Screen.EDIT_ROOM -> roomToEdit?.let { room ->
            RoomFormScreen(
                initialRoom = room,
                onSubmit = { input, onResult -> viewModel.updateRoom(room.id, input, onResult) },
                onDone = { screen = Screen.ROOMS },
                onCancel = { screen = Screen.ROOMS }
            )
        }
    }
}
