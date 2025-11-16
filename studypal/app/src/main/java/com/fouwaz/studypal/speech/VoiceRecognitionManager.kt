package com.fouwaz.studypal.speech

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sqrt

class VoiceRecognitionManager(private val context: Context) {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var recognizer: Recognizer? = null

    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady: StateFlow<Boolean> = _isModelReady.asStateFlow()

    private val _audioAmplitude = MutableStateFlow(0f)
    val audioAmplitude: StateFlow<Float> = _audioAmplitude.asStateFlow()

    private var amplitudeUpdateJob: Job? = null
    private var audioRecord: AudioRecord? = null
    private var isMonitoringAmplitude = false

    companion object {
        private const val TAG = "VoiceRecognitionManager"
        private const val SAMPLE_RATE = 16000f

        // Vosk model - you'll need to download this
        // Download from: https://alphacephei.com/vosk/models
        // Recommended: vosk-model-small-en-us-0.15.zip (40MB)
        private const val MODEL_NAME = "vosk-model-small-en-us-0.15"

        private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private fun startAmplitudeMonitoring() {
        if (isMonitoringAmplitude) return

        isMonitoringAmplitude = true
        amplitudeUpdateJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val bufferSize = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE.toInt(),
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT
                )

                audioRecord = AudioRecord(
                    AUDIO_SOURCE,
                    SAMPLE_RATE.toInt(),
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                audioRecord?.startRecording()
                val buffer = ShortArray(bufferSize)

                while (isMonitoringAmplitude) {
                    val readSize = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                    if (readSize > 0) {
                        // Calculate RMS (Root Mean Square) amplitude
                        var sum = 0.0
                        for (i in 0 until readSize) {
                            sum += (buffer[i] * buffer[i]).toDouble()
                        }
                        val rms = sqrt(sum / readSize)

                        // Normalize to 0-1 range (typical range is 0-32768 for 16-bit audio)
                        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)

                        // Apply some smoothing and sensitivity
                        val amplified = (normalized * 3f).coerceIn(0f, 1f)

                        _audioAmplitude.value = amplified
                    }

                    delay(50) // Update ~20 times per second
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error monitoring amplitude", e)
            }
        }
    }

    private fun stopAmplitudeMonitoring() {
        isMonitoringAmplitude = false
        amplitudeUpdateJob?.cancel()
        amplitudeUpdateJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping amplitude monitoring", e)
        }

        _audioAmplitude.value = 0f
    }

    /**
     * Initialize the Vosk model
     * Model will be extracted from assets or loaded from files directory
     */
    suspend fun initModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Initializing Vosk model...")

            // Check if model exists in assets
            val assetsList = context.assets.list("")?.toList() ?: emptyList()
            Log.d(TAG, "Assets root: $assetsList")

            val modelPath = File(context.filesDir, MODEL_NAME)

            // If model doesn't exist in files directory, extract from assets
            if (!modelPath.exists()) {
                Log.d(TAG, "Model not in files directory, checking assets...")

                val assetsModelFiles = context.assets.list(MODEL_NAME)?.toList()
                if (assetsModelFiles.isNullOrEmpty()) {
                    Log.e(TAG, "Model not found in assets: $MODEL_NAME")
                    _error.value = "Voice recognition model not found. Please place '$MODEL_NAME' folder in app/src/main/assets/"
                    _isModelReady.value = false
                    return@withContext false
                }

                Log.d(TAG, "Model found in assets, extracting...")
            } else {
                Log.d(TAG, "Model already exists in files directory")
            }

            // StorageService.unpack will extract from assets if needed
            var success = false
            val exception: Exception? = null

            StorageService.unpack(
                context,
                MODEL_NAME,
                MODEL_NAME,
                { model ->
                    this@VoiceRecognitionManager.model = model
                    recognizer = Recognizer(model, SAMPLE_RATE)
                    _isModelReady.value = true
                    _error.value = null
                    success = true
                    Log.d(TAG, "Model initialized successfully")
                },
                { ex ->
                    Log.e(TAG, "Failed to initialize model", ex)
                    _error.value = "Failed to load model: ${ex.message}"
                    _isModelReady.value = false
                    success = false
                }
            )

            // Wait a bit for the callback to execute
            kotlinx.coroutines.delay(1000)

            success || _isModelReady.value
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model", e)
            _error.value = "Error: ${e.message}"
            _isModelReady.value = false
            false
        }
    }

    private var completedText = ""  // Store completed/final text separately

    private val recognitionListener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            hypothesis?.let {
                try {
                    val jsonObject = JSONObject(it)
                    val partial = jsonObject.optString("partial", "")
                    if (partial.isNotEmpty()) {
                        // Show completed text + current partial (don't overwrite everything!)
                        _transcribedText.value = if (completedText.isEmpty()) {
                            partial
                        } else {
                            "$completedText $partial"
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing partial result", e)
                }
            }
        }

        override fun onResult(hypothesis: String?) {
            hypothesis?.let {
                try {
                    val jsonObject = JSONObject(it)
                    val text = jsonObject.optString("text", "")
                    if (text.isNotEmpty()) {
                        // Add to completed text
                        completedText = if (completedText.isEmpty()) {
                            text
                        } else {
                            "$completedText $text"
                        }
                        _transcribedText.value = completedText
                        Log.d(TAG, "Added result: $text (Total length: ${completedText.length})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing result", e)
                }
            }
        }

        override fun onFinalResult(hypothesis: String?) {
            onResult(hypothesis)
            Log.d(TAG, "Final result: ${_transcribedText.value}")
        }

        override fun onError(exception: Exception?) {
            Log.e(TAG, "Recognition error", exception)
            _error.value = "Recognition error: ${exception?.message}"
            _isListening.value = false
        }

        override fun onTimeout() {
            Log.d(TAG, "Recognition timeout")
            stopListening()
        }
    }

    fun startListening() {
        if (!_isModelReady.value || model == null || recognizer == null) {
            _error.value = "Model not initialized. Please initialize first."
            return
        }

        try {
            speechService = SpeechService(recognizer, SAMPLE_RATE)
            speechService?.startListening(recognitionListener)
            _isListening.value = true
            _transcribedText.value = ""
            completedText = ""  // Reset completed text for new recording
            _error.value = null

            // Start amplitude monitoring
            startAmplitudeMonitoring()

            Log.d(TAG, "Started listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recognition", e)
            _error.value = "Failed to start listening: ${e.message}"
            _isListening.value = false
        }
    }

    fun pauseListening() {
        if (!_isListening.value || _isPaused.value) return

        try {
            speechService?.stop()
            _isPaused.value = true
            Log.d(TAG, "Paused listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing recognition", e)
            _error.value = "Error pausing: ${e.message}"
        }
    }

    fun resumeListening() {
        if (!_isListening.value || !_isPaused.value) return

        try {
            speechService?.startListening(recognitionListener)
            _isPaused.value = false
            Log.d(TAG, "Resumed listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming recognition", e)
            _error.value = "Error resuming: ${e.message}"
        }
    }

    fun stopListening() {
        try {
            speechService?.stop()
            speechService?.shutdown()
            speechService = null
            _isListening.value = false
            _isPaused.value = false

            // Stop amplitude monitoring
            stopAmplitudeMonitoring()

            Log.d(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition", e)
            _error.value = "Error stopping: ${e.message}"
        }
    }

    fun clearText() {
        _transcribedText.value = ""
        completedText = ""
    }

    fun clearError() {
        _error.value = null
    }

    fun release() {
        stopListening()
        stopAmplitudeMonitoring()
        recognizer?.close()
        model?.close()
        recognizer = null
        model = null
        _isModelReady.value = false
        Log.d(TAG, "Resources released")
    }
}
