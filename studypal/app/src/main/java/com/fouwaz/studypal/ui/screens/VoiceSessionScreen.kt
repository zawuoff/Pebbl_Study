package com.fouwaz.studypal.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fouwaz.studypal.R
import com.fouwaz.studypal.domain.model.DraftTone
import com.fouwaz.studypal.domain.model.FinalDraftConfig
import com.fouwaz.studypal.domain.model.RefinementLevel
import com.fouwaz.studypal.ui.viewmodel.SessionState
import com.fouwaz.studypal.ui.viewmodel.VoiceSessionViewModel
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSessionScreen(
    projectId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToDraft: (Long) -> Unit,
    viewModel: VoiceSessionViewModel = viewModel()
) {
    // Initialize session when screen loads
    LaunchedEffect(projectId) {
        viewModel.setProject(projectId)
    }

    // Observe session state
    val sessionState by viewModel.sessionState.collectAsState()
    val streams by viewModel.streams.collectAsState()
    val currentQuestions by viewModel.currentQuestions.collectAsState()
    val selectedQuestionIndex by viewModel.selectedQuestionIndex.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val error by viewModel.error.collectAsState()
    val sessionComplete by viewModel.sessionComplete.collectAsState()
    val transcribedText by viewModel.voiceManager.transcribedText.collectAsState()
    val projectTitle by viewModel.projectTitle.collectAsState()
    val audioAmplitude by viewModel.voiceManager.audioAmplitude.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }
    val isPaused by viewModel.voiceManager.isPaused.collectAsState()
    var recordingTime by remember { mutableStateOf(0L) }

    // Bottom sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    val draftConfig by viewModel.draftConfig.collectAsState()

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

    // Handle session completion
    LaunchedEffect(sessionComplete) {
        if (sessionComplete) {
            onNavigateToDraft(projectId)
            viewModel.resetSessionComplete()
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    // Pulsing animation for recording
    val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        topBar = {
            // Custom header without visible divider
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
                    // Back button
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF000000)
                        )
                    }

                    // Centered title
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = projectTitle.ifEmpty { "Voice Session" },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF000000)
                            )
                        )
                    }

                    // Finish session tick button (only show if there are streams)
                    if (streams.isNotEmpty()) {
                        IconButton(
                            onClick = { showBottomSheet = true },
                            enabled = !isProcessing && sessionState == SessionState.Ready
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Finish Session",
                                tint = Color(0xFF000000)
                            )
                        }
                    } else {
                        // Spacer to balance when no tick button
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFFFFCF9)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // State indicator
            when (sessionState) {
                SessionState.Initializing -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Initializing voice recognition...",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                SessionState.ProcessingAI -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Getting AI follow-up question...",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                SessionState.GeneratingDraft -> {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Generating your academic draft...",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is SessionState.Error -> {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "Error: ${(sessionState as SessionState.Error).message}",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {}
            }
            // Conversation History
            val visibleStreams = streams.filter {
                it.transcribedText.isNotEmpty() || (it.aiQuestion1 != null && it.transcribedText.isEmpty())
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = false,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // Show all previous conversation items
                    items(visibleStreams) { stream ->
                        ConversationTurnItem(
                            userText = stream.transcribedText,
                            aiQuestion = stream.aiQuestion,
                            aiQuestion1 = stream.aiQuestion1,
                            aiQuestion2 = stream.aiQuestion2,
                            aiQuestion3 = stream.aiQuestion3,
                            selectedQuestionIndex = stream.selectedQuestionIndex,
                            sequenceNumber = stream.sequenceNumber
                        )
                    }

                    // Live transcription while recording (appears at the bottom of conversation)
                    if (isRecording && transcribedText.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                                color = Color(0xFFF8F8F8),
                                shadowElevation = 2.dp
                            ) {
                                Text(
                                    text = transcribedText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
                                        color = Color(0xFF2A2A37)
                                    ),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }

                    // Current AI questions (appear at the bottom of chat)
                    if (currentQuestions.isNotEmpty() && selectedQuestionIndex == null) {
                        items(
                            count = currentQuestions.size,
                            key = { index -> currentQuestions[index] } // Stable key based on question content
                        ) { index ->
                            val question = currentQuestions[index]

                            // Staggered fade-in animation - only happens ONCE per question
                            var visible by remember(question) { mutableStateOf(false) }
                            LaunchedEffect(question) {
                                kotlinx.coroutines.delay((index * 150).toLong())
                                visible = true
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = visible,
                                enter = androidx.compose.animation.fadeIn(
                                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                                ) + androidx.compose.animation.slideInVertically(
                                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it / 4 }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectQuestion(index) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color(0xFF2A2A37),
                                        modifier = Modifier
                                            .size(20.dp)
                                            .padding(top = 2.dp, end = 8.dp)
                                    )
                                    Text(
                                        question,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
                                            color = Color(0xFF2A2A37)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                val showPrompt = visibleStreams.isEmpty() &&
                        currentQuestions.isEmpty() &&
                        textInput.isEmpty() &&
                        transcribedText.isEmpty() &&
                        !isRecording &&
                        !isTyping

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
                            text = "Hello, Whats on your mind?",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
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
                val canInput = currentQuestions.isEmpty() || selectedQuestionIndex != null

                // Recording mode UI
                androidx.compose.animation.AnimatedVisibility(
                    visible = isRecording,
                    enter = androidx.compose.animation.fadeIn(tween(300)) +
                            androidx.compose.animation.expandVertically(tween(300)),
                    exit = androidx.compose.animation.fadeOut(tween(300)) +
                            androidx.compose.animation.shrinkVertically(tween(300))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Waveform animation
                        WaveformAnimation(
                            isPlaying = !isPaused,
                            audioAmplitude = audioAmplitude,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(80.dp)
                        )

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

                            // Send button
                            FloatingActionButton(
                                onClick = {
                                    viewModel.stopRecording()
                                    viewModel.submitTranscription(transcribedText)
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
                                    contentDescription = "Send",
                                    tint = Color(0xFF000000),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Normal text input mode
                androidx.compose.animation.AnimatedVisibility(
                    visible = !isRecording,
                    enter = androidx.compose.animation.fadeIn(tween(300)) +
                            androidx.compose.animation.expandVertically(tween(300)),
                    exit = androidx.compose.animation.fadeOut(tween(300)) +
                            androidx.compose.animation.shrinkVertically(tween(300))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Text Input Box
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                            color = Color(0xFFE9DED9)
                        ) {
                            androidx.compose.material3.TextField(
                                value = textInput,
                                onValueChange = {
                                    textInput = it
                                    isTyping = it.isNotEmpty()
                                },
                                modifier = Modifier.fillMaxSize(),
                                placeholder = {
                                    Text(
                                        "Add text here",
                                        color = Color(0xFF000000).copy(alpha = 0.5f)
                                    )
                                },
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFFE9DED9),
                                    unfocusedContainerColor = Color(0xFFE9DED9),
                                    disabledContainerColor = Color(0xFFE9DED9),
                                    focusedTextColor = Color(0xFF000000),
                                    unfocusedTextColor = Color(0xFF000000),
                                    cursorColor = Color(0xFF000000),
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                enabled = canInput,
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Mic or Send Button (circular)
                        FloatingActionButton(
                            onClick = {
                                if (isTyping && textInput.isNotEmpty()) {
                                    // Send text
                                    viewModel.submitTranscription(textInput)
                                    textInput = ""
                                    isTyping = false
                                } else if (canInput) {
                                    // Start recording
                                    viewModel.startRecording()
                                }
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
                                imageVector = if (isTyping && textInput.isNotEmpty()) Icons.Default.Send else Icons.Default.Mic,
                                contentDescription = if (isTyping) "Send" else "Mic",
                                tint = Color(0xFF000000),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Draft Configuration Bottom Sheet
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color(0xFFFFFCF9)
        ) {
            DraftConfigSheet(
                config = draftConfig,
                onConfigChange = { viewModel.updateDraftConfig(it) },
                onGenerate = {
                    showBottomSheet = false
                    viewModel.finishSession(it)
                },
                onCancel = { showBottomSheet = false }
            )
        }
    }
}

@Composable
private fun DraftConfigSheet(
    config: FinalDraftConfig,
    onConfigChange: (FinalDraftConfig) -> Unit,
    onGenerate: (FinalDraftConfig) -> Unit,
    onCancel: () -> Unit
) {
    var localConfig by remember(config) { mutableStateOf(config) }
    var customWordGoal by remember(localConfig.wordGoal) { mutableStateOf(localConfig.wordGoal.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            "Draft Settings",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = Color(0xFF000000),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Word Goal Section
        Text(
            "Word Goal",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF000000),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Preset chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(300, 500, 800).forEach { goal ->
                FilterChip(
                    selected = localConfig.wordGoal == goal,
                    onClick = {
                        localConfig = localConfig.copy(wordGoal = goal)
                        customWordGoal = goal.toString()
                    },
                    label = { Text("$goal words") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE9DED9),
                        selectedLabelColor = Color(0xFF000000)
                    )
                )
            }
        }

        // Custom word goal input
        OutlinedTextField(
            value = customWordGoal,
            onValueChange = {
                customWordGoal = it
                it.toIntOrNull()?.let { goal ->
                    if (goal in 100..5000) {
                        localConfig = localConfig.copy(wordGoal = goal)
                    }
                }
            },
            label = { Text("Custom word goal") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF000000),
                focusedLabelColor = Color(0xFF000000)
            )
        )

        // Tone Section
        Text(
            "Tone",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF000000),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DraftTone.values().forEach { tone ->
                FilterChip(
                    selected = localConfig.tone == tone,
                    onClick = { localConfig = localConfig.copy(tone = tone) },
                    label = { Text(tone.displayName) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE9DED9),
                        selectedLabelColor = Color(0xFF000000)
                    )
                )
            }
        }

        // Refinement Level Section
        Text(
            "Refinement",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF000000),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            RefinementLevel.values().forEach { level ->
                FilterChip(
                    selected = localConfig.refinementLevel == level,
                    onClick = { localConfig = localConfig.copy(refinementLevel = level) },
                    label = { Text(level.displayName) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE9DED9),
                        selectedLabelColor = Color(0xFF000000)
                    )
                )
            }
        }

        // Optional Toggles
        Text(
            "Additional Options",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF000000),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Include summary paragraph",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2A2A37)
            )
            Switch(
                checked = localConfig.includeSummary,
                onCheckedChange = { localConfig = localConfig.copy(includeSummary = it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF000000),
                    checkedTrackColor = Color(0xFFE9DED9),
                    uncheckedThumbColor = Color(0xFF2A2A37),
                    uncheckedTrackColor = Color(0xFFE9DED9).copy(alpha = 0.5f),
                    uncheckedBorderColor = Color(0xFF2A2A37).copy(alpha = 0.3f)
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Highlight key takeaways",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF2A2A37)
            )
            Switch(
                checked = localConfig.includeHighlights,
                onCheckedChange = { localConfig = localConfig.copy(includeHighlights = it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF000000),
                    checkedTrackColor = Color(0xFFE9DED9),
                    uncheckedThumbColor = Color(0xFF2A2A37),
                    uncheckedTrackColor = Color(0xFFE9DED9).copy(alpha = 0.5f),
                    uncheckedBorderColor = Color(0xFF2A2A37).copy(alpha = 0.3f)
                )
            )
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFF000000))
            ) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF000000)
                )
            }

            Button(
                onClick = {
                    onConfigChange(localConfig)
                    onGenerate(localConfig)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE9DED9),
                    contentColor = Color(0xFF000000)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    "Generate",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun ConversationTurnItem(
    userText: String,
    aiQuestion: String?,
    aiQuestion1: String?,
    aiQuestion2: String?,
    aiQuestion3: String?,
    selectedQuestionIndex: Int?,
    sequenceNumber: Int
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI question - show only the selected question (no background)
        if (aiQuestion1 != null && aiQuestion2 != null && aiQuestion3 != null && selectedQuestionIndex != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF2A2A37),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp, end = 8.dp)
                )
                Text(
                    aiQuestion ?: "",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
                        color = Color(0xFF2A2A37)
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // User response - only show if there's text
        if (userText.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = Color(0xFFF8F8F8),
                shadowElevation = 2.dp
            ) {
                Text(
                    userText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4f,
                        color = Color(0xFF2A2A37)
                    ),
                    modifier = Modifier.padding(16.dp)
                )
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
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
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
                    .width(38.dp)
                    .fillMaxHeight(heightFraction)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF000000),
                                Color(0xFF2A2A37)
                            )
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)
                    )
            )
        }
    }
}
