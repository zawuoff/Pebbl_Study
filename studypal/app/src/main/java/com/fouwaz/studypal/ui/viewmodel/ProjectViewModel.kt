package com.fouwaz.studypal.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fouwaz.studypal.VoiceStreamApplication
import com.fouwaz.studypal.data.local.entity.ProjectEntity
import com.fouwaz.studypal.domain.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as VoiceStreamApplication).database
    private val projectDao = database.projectDao()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadProjects()
    }

    private fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                projectDao.getAllProjects().collect { entities ->
                    _projects.value = entities.map { Project.fromEntity(it) }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.value = "Failed to load projects: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun createProject(title: String, tags: List<String> = emptyList()) {
        viewModelScope.launch {
            try {
                val project = ProjectEntity(
                    title = title,
                    tags = tags.joinToString(","),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                projectDao.insertProject(project)
            } catch (e: Exception) {
                _error.value = "Failed to create project: ${e.message}"
            }
        }
    }

    // Helper for flows that need the new project's id immediately
    suspend fun createProjectAndReturnId(title: String, tags: List<String> = emptyList()): Long {
        return try {
            val project = ProjectEntity(
                title = title,
                tags = tags.joinToString(","),
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            projectDao.insertProject(project)
        } catch (e: Exception) {
            _error.value = "Failed to create project: ${e.message}"
            -1L
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            try {
                projectDao.softDeleteProject(projectId)
            } catch (e: Exception) {
                _error.value = "Failed to delete project: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
