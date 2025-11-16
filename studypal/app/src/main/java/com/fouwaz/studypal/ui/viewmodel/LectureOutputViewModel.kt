package com.fouwaz.studypal.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fouwaz.studypal.VoiceStreamApplication
import com.fouwaz.studypal.data.local.entity.LectureEntity
import com.fouwaz.studypal.data.local.entity.LectureOutputEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LectureOutputState {
    object Loading : LectureOutputState()
    object Ready : LectureOutputState()
    data class Error(val message: String) : LectureOutputState()
}

data class LectureOutput(
    val type: String,
    val displayName: String,
    val content: String?,
    val isAvailable: Boolean
)

class LectureOutputViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as VoiceStreamApplication).database
    private val lectureDao = database.lectureDao()
    private val lectureOutputDao = database.lectureOutputDao()
    private val projectDao = database.projectDao()

    private val _state = MutableStateFlow<LectureOutputState>(LectureOutputState.Loading)
    val state: StateFlow<LectureOutputState> = _state.asStateFlow()

    private val _lecture = MutableStateFlow<LectureEntity?>(null)
    val lecture: StateFlow<LectureEntity?> = _lecture.asStateFlow()

    private val _selectedOutputType = MutableStateFlow<String?>(null)
    val selectedOutputType: StateFlow<String?> = _selectedOutputType.asStateFlow()

    private val _outputs = MutableStateFlow<List<LectureOutput>>(emptyList())
    val outputs: StateFlow<List<LectureOutput>> = _outputs.asStateFlow()

    private val _currentOutput = MutableStateFlow<LectureOutput?>(null)
    val currentOutput: StateFlow<LectureOutput?> = _currentOutput.asStateFlow()

    private val _overview = MutableStateFlow<String?>(null)
    val overview: StateFlow<String?> = _overview.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadLecture(lectureId: Long) {
        viewModelScope.launch {
            try {
                _state.value = LectureOutputState.Loading

                // Load lecture
                val lectureEntity = lectureDao.getLectureById(lectureId)
                if (lectureEntity == null) {
                    _error.value = "Lecture not found"
                    _state.value = LectureOutputState.Error("Lecture not found")
                    return@launch
                }
                _lecture.value = lectureEntity

                // Load all outputs
                lectureOutputDao.getOutputsForLecture(lectureId).collect { outputEntities ->
                    android.util.Log.d("LectureOutputVM", "Loaded ${outputEntities.size} outputs for lecture $lectureId")
                    outputEntities.forEach { entity ->
                        android.util.Log.d("LectureOutputVM", "Output type: ${entity.outputType}, content length: ${entity.content.length}")
                    }

                    // Set overview separately
                    val overviewContent = outputEntities.find { it.outputType == "overview" }?.content
                    _overview.value = overviewContent
                    android.util.Log.d("LectureOutputVM", "Overview content length: ${overviewContent?.length ?: 0}")

                    val outputList = listOf(
                        LectureOutput(
                            type = "notes",
                            displayName = "Notes",
                            content = outputEntities.find { it.outputType == "notes" }?.content,
                            isAvailable = outputEntities.any { it.outputType == "notes" }
                        ),
                        LectureOutput(
                            type = "summary",
                            displayName = "Summary",
                            content = outputEntities.find { it.outputType == "summary" }?.content,
                            isAvailable = outputEntities.any { it.outputType == "summary" }
                        ),
                        LectureOutput(
                            type = "transcription",
                            displayName = "Transcription",
                            content = lectureEntity.transcription,
                            isAvailable = !lectureEntity.transcription.isNullOrBlank()
                        )
                    )
                    _outputs.value = outputList

                    android.util.Log.d("LectureOutputVM", "Summary available: ${outputList.find { it.type == "summary" }?.isAvailable}, content length: ${outputList.find { it.type == "summary" }?.content?.length}")

                    // Set current output only if something is selected
                    if (_selectedOutputType.value != null) {
                        updateCurrentOutput()
                    }
                    _state.value = LectureOutputState.Ready
                }

            } catch (e: Exception) {
                _error.value = "Error loading lecture: ${e.message}"
                _state.value = LectureOutputState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun selectOutputType(type: String?) {
        _selectedOutputType.value = type
        updateCurrentOutput()
    }

    private fun updateCurrentOutput() {
        val selectedType = _selectedOutputType.value
        _currentOutput.value = if (selectedType != null) {
            _outputs.value.find { it.type == selectedType }
        } else {
            null
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun deleteLecture(lectureId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                val lecture = lectureDao.getLectureById(lectureId)
                lectureDao.deleteLectureById(lectureId)
                lectureOutputDao.deleteAllOutputsForLecture(lectureId)

                lecture?.projectId?.let { projectId ->
                    projectDao.getProjectById(projectId)?.let { project ->
                        val updatedIds = project.linkedLectureIds
                            .split(",")
                            .mapNotNull { it.toLongOrNull() }
                            .filter { it != lectureId }
                            .joinToString(",")

                        val updatedProject = project.copy(
                            linkedLectureIds = updatedIds,
                            updatedAt = System.currentTimeMillis()
                        )
                        projectDao.insertProject(updatedProject)
                    }
                }

                onComplete()
            } catch (e: Exception) {
                _error.value = "Failed to delete lecture: ${e.message}"
            }
        }
    }
}


