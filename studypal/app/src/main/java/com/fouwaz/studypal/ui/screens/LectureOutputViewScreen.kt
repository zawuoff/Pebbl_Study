package com.fouwaz.studypal.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fouwaz.studypal.ui.viewmodel.LectureOutputState
import com.fouwaz.studypal.ui.viewmodel.LectureOutputViewModel

/**
 * Formats markdown text to AnnotatedString with proper styling
 */
@Composable
private fun formatMarkdownText(text: String): AnnotatedString {
    return buildAnnotatedString {
        val lines = text.lines()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]

            when {
                // Headers (## or #)
                line.startsWith("###") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp)) {
                        append(line.removePrefix("###").trim())
                    }
                    append("\n\n")
                }
                line.startsWith("##") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp)) {
                        append(line.removePrefix("##").trim())
                    }
                    append("\n\n")
                }
                line.startsWith("#") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                        append(line.removePrefix("#").trim())
                    }
                    append("\n\n")
                }

                // Bullet points (- or *)
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val indent = line.takeWhile { it == ' ' }.length / 2
                    append("  ".repeat(indent))
                    append("• ")

                    // Process inline formatting in bullet point
                    val content = line.trimStart().removePrefix("- ").removePrefix("* ")
                    appendFormattedInline(content)
                    append("\n")
                }

                // Numbered lists
                line.trimStart().matches(Regex("^\\d+\\.\\s.*")) -> {
                    val indent = line.takeWhile { it == ' ' }.length / 2
                    append("  ".repeat(indent))
                    appendFormattedInline(line.trimStart())
                    append("\n")
                }

                // Q: and A: for flashcards
                line.startsWith("Q:") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Q: ")
                    }
                    appendFormattedInline(line.removePrefix("Q:").trim())
                    append("\n")
                }
                line.startsWith("A:") -> {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Normal)) {
                        append("A: ")
                    }
                    appendFormattedInline(line.removePrefix("A:").trim())
                    append("\n\n")
                }

                // Empty line
                line.isBlank() -> {
                    append("\n")
                }

                // Regular paragraph
                else -> {
                    appendFormattedInline(line)
                    append("\n")
                }
            }

            i++
        }
    }
}

/**
 * Helper to process inline formatting like **bold** and `code`
 */
private fun AnnotatedString.Builder.appendFormattedInline(text: String) {
    var processedText = text

    // Remove or replace markdown inline formatting
    // **bold** -> bold text
    processedText = processedText.replace(Regex("\\*\\*(.+?)\\*\\*")) { match ->
        match.groupValues[1]
    }

    // *italic* -> just remove asterisks
    processedText = processedText.replace(Regex("\\*(.+?)\\*")) { match ->
        match.groupValues[1]
    }

    // `code` -> just remove backticks
    processedText = processedText.replace(Regex("`(.+?)`")) { match ->
        match.groupValues[1]
    }

    // → arrow (keep as is, it's nice)
    // Just append the cleaned text
    append(processedText)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureOutputViewScreen(
    lectureId: Long,
    onNavigateBack: () -> Unit,
    viewModel: LectureOutputViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val lecture by viewModel.lecture.collectAsState()
    val selectedOutputType by viewModel.selectedOutputType.collectAsState()
    val outputs by viewModel.outputs.collectAsState()
    val currentOutput by viewModel.currentOutput.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(lectureId) {
        viewModel.loadLecture(lectureId)
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

    // Show delete confirmation
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Lecture") },
            text = { Text("Are you sure you want to delete this lecture? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteLecture(lectureId) {
                            showDeleteDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF000000)
                        )
                    }
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = lecture?.title ?: "Lecture",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF000000)
                            ),
                            maxLines = 1
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFE53935)
                        )
                    }
                }
            }
        },
        containerColor = Color(0xFFFFFCF9)
    ) { padding ->
        when (state) {
            is LectureOutputState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF000000))
                }
            }

            is LectureOutputState.Ready -> {
                val overviewText by viewModel.overview.collectAsState()

                if (selectedOutputType == null) {
                    // Show overview and card selection
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Lecture Summary Card
                        if (overviewText != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .padding(top = 20.dp)
                            ) {
                                Text(
                                    text = "Lecture Summary",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = Color(0xFF000000),
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFFFFD)
                                ) {
                                    Text(
                                        text = overviewText!!,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF000000),
                                        lineHeight = 22.sp,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        // Card buttons (3 cards stacked vertically)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            outputs.forEach { output ->
                                OutputCard(
                                    label = output.displayName,
                                    isAvailable = output.isAvailable,
                                    onClick = {
                                        if (output.isAvailable) {
                                            viewModel.selectOutputType(output.type)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Regenerate and Done buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Regenerate button
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable { /* TODO: Implement regenerate */ },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1EAE0)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Regenerate",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color(0xFF000000)
                                    )
                                }
                            }

                            // Done button
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable { onNavigateBack() },
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF000000)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Done",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = Color(0xFFFFFFFF)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Show selected output content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        // Back to overview button
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .clickable { viewModel.selectOutputType(null) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE9DED9).copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "← Back to Overview",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = Color(0xFF000000)
                                )
                            }
                        }

                        // Output content
                        if (currentOutput?.isAvailable == true && currentOutput?.content != null) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(horizontal = 20.dp)
                                    .padding(bottom = 20.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFF8F8F8),
                                shadowElevation = 2.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(20.dp)
                                ) {
                                    Text(
                                        text = currentOutput!!.displayName,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = Color(0xFF000000),
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    Text(
                                        text = formatMarkdownText(currentOutput!!.content!!),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color(0xFF000000),
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (currentOutput?.isAvailable == false)
                                        "This output is not available yet"
                                    else
                                        "No content available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF2A2A37).copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            is LectureOutputState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Error loading lecture",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF000000)
                        )
                        Text(
                            text = (state as LectureOutputState.Error).message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2A2A37).copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputCard(
    label: String,
    isAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(120.dp)
            .clickable(enabled = isAvailable, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isAvailable) Color(0xFFF1EAE0) else Color(0xFFF8F8F8),
        shadowElevation = if (isAvailable) 2.dp else 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = if (isAvailable) Color(0xFF000000) else Color(0xFF2A2A37).copy(alpha = 0.3f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
