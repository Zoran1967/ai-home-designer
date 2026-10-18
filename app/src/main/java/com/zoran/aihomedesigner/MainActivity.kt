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
import com.zoran.aihomedesigner.domain.Project
import com.zoran.aihomedesigner.ui.CreateProjectScreen
import com.zoran.aihomedesigner.ui.CreateRoomScreen
import com.zoran.aihomedesigner.ui.ProjectListScreen
import com.zoran.aihomedesigner.ui.ProjectsViewModel
import com.zoran.aihomedesigner.ui.RoomListScreen

private enum class Screen { PROJECTS, CREATE_PROJECT, ROOMS, CREATE_ROOM }

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
    val projects by viewModel.projects.collectAsState()
    val selectedProject = projects?.firstOrNull { it.id == selectedProjectId }

    BackHandler(enabled = screen != Screen.PROJECTS) {
        screen = if (screen == Screen.CREATE_ROOM) Screen.ROOMS else Screen.PROJECTS
    }

    // Ako je izabrani projekat nestao (npr. obrisan), vrati se na listu projekata.
    LaunchedEffect(projects, selectedProjectId, screen) {
        val needsProject = screen == Screen.ROOMS || screen == Screen.CREATE_ROOM
        if (needsProject && projects != null && selectedProject == null) {
            screen = Screen.PROJECTS
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
            RoomsRoute(
                project = project,
                viewModel = viewModel,
                onBack = { screen = Screen.PROJECTS },
                onAddRoom = { screen = Screen.CREATE_ROOM }
            )
        }

        Screen.CREATE_ROOM -> selectedProject?.let { project ->
            CreateRoomScreen(
                onCreate = { input, onResult -> viewModel.createRoom(project.id, input, onResult) },
                onDone = { screen = Screen.ROOMS },
                onCancel = { screen = Screen.ROOMS }
            )
        }
    }
}

@Composable
private fun RoomsRoute(
    project: Project,
    viewModel: ProjectsViewModel,
    onBack: () -> Unit,
    onAddRoom: () -> Unit
) {
    val roomsFlow = remember(project.id) { viewModel.observeRooms(project.id) }
    val rooms by roomsFlow.collectAsState(initial = null)

    RoomListScreen(
        projectName = project.name,
        rooms = rooms,
        onBack = onBack,
        onAddRoom = onAddRoom,
        onDeleteRoom = { viewModel.deleteRoom(it.id) }
    )
}
