package com.fouwaz.studypal.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fouwaz.studypal.VoiceStreamApplication
import com.fouwaz.studypal.data.local.entity.LectureEntity
import com.fouwaz.studypal.data.local.entity.LectureOutputEntity
import com.fouwaz.studypal.data.repository.AiRepository
import com.fouwaz.studypal.speech.VoiceRecognitionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LectureRecordingState {
    object Idle : LectureRecordingState()
    object Initializing : LectureRecordingState()
    object Ready : LectureRecordingState()
    object Recording : LectureRecordingState()
    object Processing : LectureRecordingState()
    object GeneratingOutputs : LectureRecordingState()
    data class Complete(val lectureId: Long) : LectureRecordingState()
    data class Error(val message: String) : LectureRecordingState()
}

class LectureRecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as VoiceStreamApplication).database
    private val lectureDao = database.lectureDao()
    private val lectureOutputDao = database.lectureOutputDao()
    private val aiRepository = AiRepository()

    val voiceManager = VoiceRecognitionManager(application)

    private val _recordingState = MutableStateFlow<LectureRecordingState>(LectureRecordingState.Idle)
    val recordingState: StateFlow<LectureRecordingState> = _recordingState.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isModelInitialized = MutableStateFlow(false)
    val isModelInitialized: StateFlow<Boolean> = _isModelInitialized.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0)
    val recordingDuration: StateFlow<Int> = _recordingDuration.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _transcriptionText = MutableStateFlow("")
    val transcriptionText: StateFlow<String> = _transcriptionText.asStateFlow()

    private val _generationProgress = MutableStateFlow("")
    val generationProgress: StateFlow<String> = _generationProgress.asStateFlow()

    private var recordingStartTime = 0L

    init {
        // Observe voice recognition model readiness
        viewModelScope.launch {
            voiceManager.isModelReady.collect { ready ->
                _isModelInitialized.value = ready
                if (ready && _recordingState.value == LectureRecordingState.Initializing) {
                    _recordingState.value = LectureRecordingState.Ready
                }
            }
        }

        // Observe voice recognition errors
        viewModelScope.launch {
            voiceManager.error.collect { errorMsg ->
                if (errorMsg != null) {
                    _error.value = errorMsg
                    _recordingState.value = LectureRecordingState.Error(errorMsg)
                }
            }
        }

        // Observe transcription text (background transcription)
        viewModelScope.launch {
            voiceManager.transcribedText.collect { text ->
                _transcriptionText.value = text
            }
        }
    }

    fun initializeRecording() {
        _recordingState.value = LectureRecordingState.Initializing
        viewModelScope.launch {
            try {
                val success = voiceManager.initModel()
                if (!success) {
                    _error.value = "Failed to load voice recognition model"
                    _recordingState.value = LectureRecordingState.Error("Model initialization failed")
                }
            } catch (e: Exception) {
                _error.value = "Error initializing voice model: ${e.message}"
                _recordingState.value = LectureRecordingState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun startRecording() {
        if (_recordingState.value != LectureRecordingState.Ready) return

        _recordingState.value = LectureRecordingState.Recording
        _isRecording.value = true
        recordingStartTime = System.currentTimeMillis()
        _recordingDuration.value = 0
        _transcriptionText.value = ""

        viewModelScope.launch {
            try {
                voiceManager.startListening()
                // Track duration
                while (_isRecording.value) {
                    kotlinx.coroutines.delay(1000)
                    _recordingDuration.value = ((System.currentTimeMillis() - recordingStartTime) / 1000).toInt()
                }
            } catch (e: Exception) {
                _error.value = "Failed to start recording: ${e.message}"
                _recordingState.value = LectureRecordingState.Error(e.message ?: "Recording error")
                _isRecording.value = false
            }
        }
    }

    fun pauseRecording() {
        if (_recordingState.value != LectureRecordingState.Recording || _isRecording.value.not()) return
        viewModelScope.launch {
            try {
                voiceManager.pauseListening()
                _isRecording.value = false
            } catch (e: Exception) {
                _error.value = "Failed to pause recording: ${e.message}"
            }
        }
    }

    fun resumeRecording() {
        if (_recordingState.value != LectureRecordingState.Recording || _isRecording.value) return
        viewModelScope.launch {
            try {
                voiceManager.resumeListening()
                _isRecording.value = true
            } catch (e: Exception) {
                _error.value = "Failed to resume recording: ${e.message}"
            }
        }
    }

    fun stopRecording() {
        if (_recordingState.value != LectureRecordingState.Recording) return

        _recordingState.value = LectureRecordingState.Processing
        _isRecording.value = false

        viewModelScope.launch {
            try {
                voiceManager.stopListening()
                _generationProgress.value = "Processing recording..."
                val transcription = voiceManager.transcribedText.value
                val duration = _recordingDuration.value
                saveLectureAndGenerateOutputs(transcription, duration)
            } catch (e: Exception) {
                _error.value = "Failed to stop recording: ${e.message}"
                _recordingState.value = LectureRecordingState.Error(e.message ?: "Stop recording error")
            }
        }
    }

    private suspend fun saveLectureAndGenerateOutputs(transcription: String, duration: Int) {
        viewModelScope.launch {
            _generationProgress.value = "Saving lecture..."
        }

        viewModelScope.launch {
            try {
                _recordingState.value = LectureRecordingState.GeneratingOutputs
                // Count words
                val wordCount = transcription.split(Regex("\\s+")).filter { it.isNotEmpty() }.size

                // Generate a title from first few words
                val title = transcription.split(" ").take(5).joinToString(" ") + "..."

                // Save lecture to database
                val lectureEntity = LectureEntity(
                    title = title,
                    transcription = transcription,
                    durationSeconds = duration,
                    wordCount = wordCount
                )
                val lectureId = lectureDao.insertLecture(lectureEntity)

                // Generate all outputs in a single API call for better performance and cost
                _generationProgress.value = "Generating all outputs..."
                android.util.Log.d("LectureRecording", "Generating all outputs in one API call for lecture $lectureId")

                val result = aiRepository.generateAllOutputs(transcription)

                result.onSuccess { outputs ->
                    android.util.Log.d("LectureRecording", "All outputs generated successfully")

                    // Save overview
                    _generationProgress.value = "Saving overview..."
                    val overviewEntity = LectureOutputEntity(
                        lectureId = lectureId,
                        outputType = "overview",
                        content = outputs.overview
                    )
                    lectureOutputDao.insertOutput(overviewEntity)
                    android.util.Log.d("LectureRecording", "Overview saved (${outputs.overview.length} chars)")

                    // Save notes
                    _generationProgress.value = "Saving notes..."
                    val notesEntity = LectureOutputEntity(
                        lectureId = lectureId,
                        outputType = "notes",
                        content = outputs.notes
                    )
                    lectureOutputDao.insertOutput(notesEntity)
                    android.util.Log.d("LectureRecording", "Notes saved (${outputs.notes.length} chars)")

                    // Save summary
                    _generationProgress.value = "Saving summary..."
                    val summaryEntity = LectureOutputEntity(
                        lectureId = lectureId,
                        outputType = "summary",
                        content = outputs.summary
                    )
                    lectureOutputDao.insertOutput(summaryEntity)
                    android.util.Log.d("LectureRecording", "Summary saved (${outputs.summary.length} chars)")

                    _generationProgress.value = "Complete!"
                    _recordingState.value = LectureRecordingState.Complete(lectureId)
                }.onFailure { e ->
                    android.util.Log.e("LectureRecording", "Failed to generate all outputs", e)
                    _error.value = "Failed to generate outputs: ${e.message}"
                    _recordingState.value = LectureRecordingState.Error(e.message ?: "Generation error")
                }

            } catch (e: Exception) {
                android.util.Log.e("LectureRecording", "Exception in saveLectureAndGenerateOutputs", e)
                _error.value = "Failed to save lecture: ${e.message}"
                _recordingState.value = LectureRecordingState.Error(e.message ?: "Save error")
            }
        }
    }

    private suspend fun generateOutput(lectureId: Long, outputType: String, transcription: String) {
        try {
            android.util.Log.d("LectureRecording", "Generating $outputType for lecture $lectureId")
            val result = when (outputType) {
                "overview" -> aiRepository.generateOverview(transcription)
                "notes" -> aiRepository.generateNotes(transcription)
                "summary" -> aiRepository.generateSummary(transcription)
                else -> return
            }

            result.onSuccess { content ->
                android.util.Log.d("LectureRecording", "Successfully generated $outputType (${content.length} chars)")
                val outputEntity = LectureOutputEntity(
                    lectureId = lectureId,
                    outputType = outputType,
                    content = content
                )
                val insertedId = lectureOutputDao.insertOutput(outputEntity)
                android.util.Log.d("LectureRecording", "Inserted $outputType with ID: $insertedId")
            }.onFailure { e ->
                // Log error but don't fail the whole process
                android.util.Log.e("LectureRecording", "Failed to generate $outputType", e)
                _error.value = "Failed to generate $outputType: ${e.message}"
            }
        } catch (e: Exception) {
            android.util.Log.e("LectureRecording", "Error generating $outputType", e)
            _error.value = "Error generating $outputType: ${e.message}"
        }
    }

    fun resetState() {
        _recordingState.value = LectureRecordingState.Ready
        _transcriptionText.value = ""
        _recordingDuration.value = 0
        _generationProgress.value = ""
        voiceManager.clearText()
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}
