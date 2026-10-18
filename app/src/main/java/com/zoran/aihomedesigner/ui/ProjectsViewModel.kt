package com.zoran.aihomedesigner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zoran.aihomedesigner.data.AppDatabase
import com.zoran.aihomedesigner.data.CreateProjectResult
import com.zoran.aihomedesigner.data.ProjectRepository
import com.zoran.aihomedesigner.domain.Project
import com.zoran.aihomedesigner.domain.PropertyType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(AppDatabase.get(application).projectDao())

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
            repository.deleteProject(id)
        }
    }
}
