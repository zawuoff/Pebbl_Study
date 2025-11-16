package com.fouwaz.studypal.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.fouwaz.studypal.VoiceStreamApplication
import com.fouwaz.studypal.data.local.entity.DraftEntity
import com.fouwaz.studypal.data.local.preferences.DraftConfigPreferences
import com.fouwaz.studypal.domain.model.FinalDraftConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DraftViewScreen(
    projectId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = (context.applicationContext as VoiceStreamApplication).database
    val draftDao = database.draftDao()
    val projectDao = database.projectDao()
    val draftConfigPreferences = remember { DraftConfigPreferences(context) }

    var draft by remember { mutableStateOf<DraftEntity?>(null) }
    var projectTitle by remember { mutableStateOf("") }
    var draftConfig by remember { mutableStateOf<FinalDraftConfig?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var isVisible by remember { mutableStateOf(false) }

    // Load draft on screen load
    LaunchedEffect(projectId) {
        isLoading = true
        try {
            // Get project title
            projectDao.getProjectById(projectId)?.let {
                projectTitle = it.title
            }

            // Get current draft
            draft = draftDao.getCurrentDraft(projectId)

            // Load draft config
            draftConfig = draftConfigPreferences.getConfig(projectId).first()

            if (draft == null) {
                error = "No draft found for this project. Please complete a voice session first."
            }
        } catch (e: Exception) {
            error = "Failed to load draft: ${e.message}"
        } finally {
            isLoading = false
            isVisible = true
        }
    }

    // Fade-in animation
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(600),
        label = "alpha"
    )

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = androidx.compose.ui.graphics.Color(0xFFFFFCF9)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = androidx.compose.ui.graphics.Color(0xFF000000)
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Draft Generated",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                color = androidx.compose.ui.graphics.Color(0xFF000000)
                            )
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = androidx.compose.ui.graphics.Color(0xFFFFFCF9)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Back to Projects")
                        }
                    }
                }
                draft != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .padding(top = 16.dp, bottom = 24.dp)
                            .verticalScroll(rememberScrollState())
                            .graphicsLayer { this.alpha = alpha }
                    ) {
                        // Project Title
                        Text(
                            projectTitle,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            ),
                            color = androidx.compose.ui.graphics.Color(0xFF000000)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            "Generated Academic Draft",
                            style = MaterialTheme.typography.titleMedium,
                            color = androidx.compose.ui.graphics.Color(0xFF2A2A37)
                        )


                        Spacer(modifier = Modifier.height(32.dp))

                        // Draft Content Card with premium styling
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 2.dp,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = androidx.compose.ui.graphics.Color(0xFFF8F8F8)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    draft!!.content,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.6f
                                    ),
                                    color = androidx.compose.ui.graphics.Color(0xFF2A2A37)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Draft metadata with improved styling
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = androidx.compose.ui.graphics.Color(0xFFE9DED9).copy(alpha = 0.5f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            shadowElevation = 0.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Top row - Version and Word Count
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            "Version",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.ui.graphics.Color(0xFF2A2A37)
                                        )
                                        Text(
                                            "${draft!!.version}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                            ),
                                            color = androidx.compose.ui.graphics.Color(0xFF000000)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            "Word Count",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = androidx.compose.ui.graphics.Color(0xFF2A2A37)
                                        )
                                        Text(
                                            "${draft!!.content.split(" ").size}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                            ),
                                            color = androidx.compose.ui.graphics.Color(0xFF000000)
                                        )
                                    }
                                }

                                // Config metadata if available
                                draftConfig?.let { config ->
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        color = androidx.compose.ui.graphics.Color(0xFF000000).copy(alpha = 0.1f)
                                    )

                                    // Config chips row
                                    FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AssistChip(
                                            onClick = { },
                                            label = {
                                                Text(
                                                    "Goal: ${config.wordGoal}w",
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = androidx.compose.ui.graphics.Color(0xFFE9DED9),
                                                labelColor = androidx.compose.ui.graphics.Color(0xFF000000)
                                            ),
                                            border = null
                                        )
                                        AssistChip(
                                            onClick = { },
                                            label = {
                                                Text(
                                                    config.tone.displayName,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = androidx.compose.ui.graphics.Color(0xFFE9DED9),
                                                labelColor = androidx.compose.ui.graphics.Color(0xFF000000)
                                            ),
                                            border = null
                                        )
                                        AssistChip(
                                            onClick = { },
                                            label = {
                                                Text(
                                                    config.refinementLevel.displayName,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = androidx.compose.ui.graphics.Color(0xFFE9DED9),
                                                labelColor = androidx.compose.ui.graphics.Color(0xFF000000)
                                            ),
                                            border = null
                                        )
                                        if (config.includeSummary) {
                                            AssistChip(
                                                onClick = { },
                                                label = {
                                                    Text(
                                                        "With Summary",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = androidx.compose.ui.graphics.Color(0xFFE9DED9),
                                                    labelColor = androidx.compose.ui.graphics.Color(0xFF000000)
                                                ),
                                                border = null
                                            )
                                        }
                                        if (config.includeHighlights) {
                                            AssistChip(
                                                onClick = { },
                                                label = {
                                                    Text(
                                                        "With Highlights",
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = androidx.compose.ui.graphics.Color(0xFFE9DED9),
                                                    labelColor = androidx.compose.ui.graphics.Color(0xFF000000)
                                                ),
                                                border = null
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Primary Save action with premium button styling
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = exportToTxt(context, draft!!.content, projectTitle)
                                    if (result) {
                                        snackbarHostState.showSnackbar("Saved to Downloads/${projectTitle}.txt")
                                    } else {
                                        snackbarHostState.showSnackbar("Save failed")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = androidx.compose.ui.graphics.Color(0xFFE9DED9),
                                contentColor = androidx.compose.ui.graphics.Color(0xFF000000)
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 2.dp
                            )
                        ) {
                            Text(
                                "SAVE",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}


private fun exportToTxt(context: Context, content: String, fileName: String): Boolean {
    return try {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val file = File(downloadsDir, "${fileName.replace(" ", "_")}.txt")

        FileOutputStream(file).use { outputStream ->
            outputStream.write(content.toByteArray())
        }

        // Notify media scanner
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = android.net.Uri.fromFile(file)
        context.sendBroadcast(intent)

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

