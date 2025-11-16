package com.fouwaz.studypal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fouwaz.studypal.VoiceStreamApplication
import com.fouwaz.studypal.data.local.entity.LectureEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LectureListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToRecording: () -> Unit,
    onLectureClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val database = (context.applicationContext as VoiceStreamApplication).database
    val lectureDao = database.lectureDao()

    var lectures by remember { mutableStateOf<List<LectureEntity>>(emptyList()) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        lectureDao.getAllLectures().collect { lectureList ->
            lectures = lectureList
            isVisible = true
        }
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
                            text = "Lectures",
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToRecording,
                containerColor = Color(0xFFE9DED9),
                contentColor = Color(0xFF000000),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Record lecture")
            }
        },
        containerColor = Color(0xFFFFFCF9)
    ) { padding ->
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
        ) {
            if (lectures.isEmpty()) {
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
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color(0xFF2A2A37).copy(alpha = 0.3f)
                        )
                        Text(
                            text = "No lectures yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF2A2A37).copy(alpha = 0.6f)
                        )
                        Text(
                            text = "Tap + to record your first lecture",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2A2A37).copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(lectures) { lecture ->
                        LectureCard(
                            lecture = lecture,
                            onClick = { onLectureClick(lecture.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LectureCard(
    lecture: LectureEntity,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8F8F8),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Title
            Text(
                text = lecture.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF000000),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Duration
                StatChip(
                    label = "Duration",
                    value = formatDuration(lecture.durationSeconds)
                )

                // Words
                StatChip(
                    label = "Words",
                    value = lecture.wordCount.toString()
                )
            }

            // Date
            Text(
                text = formatDate(lecture.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF2A2A37).copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF2A2A37).copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Color(0xFF000000)
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return if (minutes > 0) {
        "${minutes}m ${secs}s"
    } else {
        "${secs}s"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
