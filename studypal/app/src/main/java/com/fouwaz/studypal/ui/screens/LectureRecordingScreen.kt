package com.fouwaz.studypal.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fouwaz.studypal.R
import com.fouwaz.studypal.ui.viewmodel.LectureRecordingState
import com.fouwaz.studypal.ui.viewmodel.LectureRecordingViewModel
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureRecordingScreen(
    projectId: Long?,
    onNavigateBack: () -> Unit,
    onRecordingComplete: (Long) -> Unit,
    viewModel: LectureRecordingViewModel = viewModel()
) {
    val recordingState by viewModel.recordingState.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val recordingDuration by viewModel.recordingDuration.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val error by viewModel.error.collectAsState()

    // Get audio amplitude for waveform animation
    val audioAmplitude by viewModel.voiceManager.audioAmplitude.collectAsState()
    val isPaused by viewModel.voiceManager.isPaused.collectAsState()

    var recordingTime by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        viewModel.initializeRecording()
    }

    // Timer for recording
    LaunchedEffect(isRecording, isPaused) {
        if (isRecording && !isPaused) {
            val startTime = System.currentTimeMillis() - recordingTime
            while (isRecording && !isPaused) {
                recordingTime = System.currentTimeMillis() - startTime
                kotlinx.coroutines.delay(100)
            }
        }
    }

    // Reset timer when recording stops
    LaunchedEffect(isRecording) {
        if (!isRecording) {
            recordingTime = 0L
        }
    }

    // Handle completion
    LaunchedEffect(recordingState) {
        if (recordingState is LectureRecordingState.Complete) {
            val lectureId = (recordingState as LectureRecordingState.Complete).lectureId
            onRecordingComplete(lectureId)
        }
    }

    // Show error dialog
    error?.let { errorMsg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(errorMsg) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFFFFCF9)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !isRecording && recordingState !is LectureRecordingState.GeneratingOutputs
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF000000)
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Lecture Recording",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF000000)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        },
        containerColor = Color(0xFFFFFCF9)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (recordingState) {
                is LectureRecordingState.Initializing -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Initializing voice recognition...",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is LectureRecordingState.Processing, is LectureRecordingState.GeneratingOutputs -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        if (recordingState is LectureRecordingState.Processing)
                            "Processing lecture..."
                        else
                            generationProgress.ifEmpty { "Generating study materials..." },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is LectureRecordingState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "Error: ${(recordingState as LectureRecordingState.Error).message}",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {}
            }

            // Main content area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                // Show prompt when not recording
                val showPrompt = !isRecording &&
                    recordingState !is LectureRecordingState.GeneratingOutputs &&
                    recordingState !is LectureRecordingState.Processing

                androidx.compose.animation.AnimatedVisibility(
                    visible = showPrompt,
                    enter = androidx.compose.animation.fadeIn(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ),
                    exit = androidx.compose.animation.fadeOut(
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.only_pebbl),
                            contentDescription = null,
                            modifier = Modifier.size(160.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Tap the mic to start recording",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Waveform animation (centered when recording)
                androidx.compose.animation.AnimatedVisibility(
                    visible = isRecording,
                    enter = androidx.compose.animation.fadeIn(tween(400)) +
                            androidx.compose.animation.scaleIn(tween(400, easing = FastOutSlowInEasing)),
                    exit = androidx.compose.animation.fadeOut(tween(300)) +
                            androidx.compose.animation.scaleOut(tween(300, easing = FastOutSlowInEasing)),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    WaveformAnimation(
                        isPlaying = !isPaused,
                        audioAmplitude = audioAmplitude,
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(200.dp)
                    )
                }
            }

            // Floating input area at the bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Recording mode UI
                androidx.compose.animation.AnimatedVisibility(
                    visible = isRecording,
                    enter = androidx.compose.animation.fadeIn(tween(300)) +
                            androidx.compose.animation.expandVertically(tween(300)),
                    exit = androidx.compose.animation.fadeOut(tween(300)) +
                            androidx.compose.animation.shrinkVertically(tween(300))
                ) {
                    // Controls row
                    Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Pause/Resume button
                            FloatingActionButton(
                                onClick = {
                                    if (isPaused) {
                                        viewModel.voiceManager.resumeListening()
                                    } else {
                                        viewModel.voiceManager.pauseListening()
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                                containerColor = Color(0xFFE9DED9),
                                shape = CircleShape,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause",
                                    tint = Color(0xFF000000),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Timer display
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                color = Color(0xFFE9DED9)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val minutes = (recordingTime / 60000).toInt()
                                    val seconds = ((recordingTime % 60000) / 1000).toInt()
                                    Text(
                                        text = String.format("%d:%02d", minutes, seconds),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF000000)
                                        )
                                    )
                                }
                            }

                            // Stop button
                            FloatingActionButton(
                                onClick = {
                                    viewModel.stopRecording()
                                },
                                modifier = Modifier.size(56.dp),
                                containerColor = Color(0xFFE9DED9),
                                shape = CircleShape,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Stop",
                                    tint = Color(0xFF000000),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                }

                // Normal mode - just mic button
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isRecording && recordingState == LectureRecordingState.Ready,
                    enter = androidx.compose.animation.fadeIn(tween(300)) +
                            androidx.compose.animation.expandVertically(tween(300)),
                    exit = androidx.compose.animation.fadeOut(tween(300)) +
                            androidx.compose.animation.shrinkVertically(tween(300))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mic Button (circular)
                        FloatingActionButton(
                            onClick = {
                                viewModel.startRecording()
                            },
                            modifier = Modifier.size(56.dp),
                            containerColor = Color(0xFFE9DED9),
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Start recording",
                                tint = Color(0xFF000000),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformAnimation(
    isPlaying: Boolean,
    audioAmplitude: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 4
    val baseHeightFraction = if (isPlaying) 0.18f else 0.12f
    val maxHeightFraction = 1f
    val smoothedAmplitude by animateFloatAsState(
        targetValue = if (isPlaying) audioAmplitude.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "wave_amp"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { index ->
            val playful by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 260 + (index * 70),
                        easing = FastOutSlowInEasing
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$index"
            )

            val amplitudeResponse = if (isPlaying) smoothedAmplitude else 0f
            val easedAmplitude = sqrt(amplitudeResponse.coerceIn(0f, 1f))
            val envelope = 0.35f + (playful * 0.72f)
            val heightFraction = (baseHeightFraction +
                    envelope * easedAmplitude * (maxHeightFraction - baseHeightFraction))
                .coerceIn(baseHeightFraction, maxHeightFraction)

            Box(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight(heightFraction)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF000000),
                                Color(0xFF2A2A37)
                            )
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(30.dp)
                    )
            )
        }
    }
}

