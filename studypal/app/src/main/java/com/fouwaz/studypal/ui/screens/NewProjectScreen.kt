package com.fouwaz.studypal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fouwaz.studypal.ui.viewmodel.ProjectViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    onBack: () -> Unit,
    onProjectCreatedNavigateToSession: (Long) -> Unit,
    viewModel: ProjectViewModel = viewModel()
) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var extra by remember { mutableStateOf(TextFieldValue("")) }
    var type by remember { mutableStateOf("Essay") }
    val scope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
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
                    IconButton(onClick = onBack) {
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
                            text = "New Project",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 24.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Title field
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400)) +
                            slideInVertically(
                                animationSpec = tween(400),
                                initialOffsetY = { it / 4 }
                            )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Title",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF000000)
                            )
                        )
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = {
                                Text(
                                    "Essay on Climate Change",
                                    color = Color(0xFF2A2A37).copy(alpha = 0.5f)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF000000),
                                unfocusedBorderColor = Color(0xFF000000).copy(alpha = 0.3f),
                                focusedTextColor = Color(0xFF000000),
                                unfocusedTextColor = Color(0xFF000000),
                                cursorColor = Color(0xFF000000)
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                // Type chips
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                            slideInVertically(
                                animationSpec = tween(400, delayMillis = 100),
                                initialOffsetY = { it / 4 }
                            )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val types = listOf("Essay", "Thesis", "Research Notes")
                            types.forEach { option ->
                                FilterChip(
                                    selected = type == option,
                                    onClick = { type = option },
                                    label = {
                                        Text(
                                            option,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFE9DED9),
                                        selectedLabelColor = Color(0xFF000000),
                                        containerColor = Color(0xFFFFFCF9),
                                        labelColor = Color(0xFF2A2A37)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // Optional section
                AnimatedVisibility(
                    visible = isVisible,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 200)) +
                            slideInVertically(
                                animationSpec = tween(400, delayMillis = 200),
                                initialOffsetY = { it / 4 }
                            )
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Optional",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2A2A37)
                            )
                        )
                        OutlinedTextField(
                            value = extra,
                            onValueChange = { extra = it },
                            placeholder = {
                                Text(
                                    "Notes, constraints, citation style…",
                                    color = Color(0xFF2A2A37).copy(alpha = 0.5f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF000000),
                                unfocusedBorderColor = Color(0xFF000000).copy(alpha = 0.3f),
                                focusedTextColor = Color(0xFF000000),
                                unfocusedTextColor = Color(0xFF000000),
                                cursorColor = Color(0xFF000000)
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium,
                            maxLines = 6
                        )
                    }
                }
            }

            // Next Button at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
            ) {
                Button(
                    onClick = {
                        if (title.text.isBlank()) return@Button
                        scope.launch {
                            val id = viewModel.createProjectAndReturnId(
                                title = title.text,
                                tags = listOf(type).filter { it.isNotEmpty() }
                            )
                            if (id > 0) onProjectCreatedNavigateToSession(id)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = title.text.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE9DED9),
                        contentColor = Color(0xFF000000),
                        disabledContainerColor = Color(0xFFE9DED9).copy(alpha = 0.5f),
                        disabledContentColor = Color(0xFF000000).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 2.dp
                    )
                ) {
                    Text(
                        "Next",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

