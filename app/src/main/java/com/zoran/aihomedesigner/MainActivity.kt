package com.zoran.aihomedesigner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zoran.aihomedesigner.ui.CreateProjectScreen
import com.zoran.aihomedesigner.ui.ProjectListScreen
import com.zoran.aihomedesigner.ui.ProjectsViewModel

private enum class Screen { LIST, CREATE }

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
    var screen by rememberSaveable { mutableStateOf(Screen.LIST) }
    val projects by viewModel.projects.collectAsState()

    BackHandler(enabled = screen == Screen.CREATE) {
        screen = Screen.LIST
    }

    when (screen) {
        Screen.LIST -> ProjectListScreen(
            projects = projects,
            onNewProject = { screen = Screen.CREATE },
            onDeleteProject = { viewModel.deleteProject(it.id) }
        )

        Screen.CREATE -> CreateProjectScreen(
            onCreate = { name, type, onResult -> viewModel.createProject(name, type, onResult) },
            onDone = { screen = Screen.LIST },
            onCancel = { screen = Screen.LIST }
        )
    }
}
