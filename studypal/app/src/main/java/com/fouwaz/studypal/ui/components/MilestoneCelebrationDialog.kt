package com.fouwaz.studypal.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fouwaz.studypal.domain.model.PebbleType
import kotlinx.coroutines.delay

@Composable
fun MilestoneCelebrationDialog(
    pebble: PebbleType,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var pebbleScale by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
        // Animate pebble dropping in
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) { value, _ ->
            pebbleScale = value
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000).copy(alpha = 0.7f))
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFFFFCF9),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Celebration header
                    Text(
                        text = "Milestone Reached!",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF000000),
                        textAlign = TextAlign.Center
                    )

                    // Pebble illustration (placeholder circle for now)
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pebbleScale)
                            .background(pebble.color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Placeholder - will be replaced with actual pebble image
                        Text(
                            text = "🪨",
                            style = MaterialTheme.typography.displayLarge
                        )
                    }

                    // Pebble name
                    Text(
                        text = pebble.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF000000),
                        textAlign = TextAlign.Center
                    )

                    // Pebble description
                    Text(
                        text = pebble.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF2A2A37).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    // Word milestone
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE9DED9).copy(alpha = 0.4f)
                    ) {
                        Text(
                            text = "${pebble.wordMilestone} words written",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF000000),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dismiss button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE9DED9),
                            contentColor = Color(0xFF000000)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 2.dp
                        )
                    ) {
                        Text(
                            "Awesome!",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
