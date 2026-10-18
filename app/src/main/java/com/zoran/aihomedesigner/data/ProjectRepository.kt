package com.zoran.aihomedesigner.data

import com.zoran.aihomedesigner.domain.Project
import com.zoran.aihomedesigner.domain.ProjectNameError
import com.zoran.aihomedesigner.domain.ProjectValidator
import com.zoran.aihomedesigner.domain.PropertyType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface CreateProjectResult {
    data class Success(val project: Project) : CreateProjectResult
    data class Invalid(val error: ProjectNameError) : CreateProjectResult
}

class ProjectRepository(private val dao: ProjectDao) {

    fun observeProjects(): Flow<List<Project>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    /** Proverava ime, pa tek onda upisuje projekat u bazu. */
    suspend fun createProject(
        rawName: String,
        type: PropertyType,
        nowMillis: Long = System.currentTimeMillis()
    ): CreateProjectResult {
        val error = ProjectValidator.validateName(rawName, dao.getAllNames())
        if (error != null) return CreateProjectResult.Invalid(error)

        val project = Project(
            id = Project.newId(),
            name = ProjectValidator.normalizeName(rawName),
            type = type,
            createdAtMillis = nowMillis
        )
        dao.insert(project.toEntity())
        return CreateProjectResult.Success(project)
    }

    suspend fun deleteProject(id: String) = dao.deleteById(id)
}
