package com.zoran.aihomedesigner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zoran.aihomedesigner.data.AppDatabase
import com.zoran.aihomedesigner.data.CreateProjectResult
import com.zoran.aihomedesigner.data.ProjectRepository
import com.zoran.aihomedesigner.data.RoomRepository
import com.zoran.aihomedesigner.data.RoomSaveResult
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

    fun deleteRoom(id: String) {
        viewModelScope.launch {
            roomRepository.deleteRoom(id)
        }
    }
}
