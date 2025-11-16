package com.fouwaz.studypal.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun WelcomeScreen(
    onStart: () -> Unit
) {
    // Breathing animation for the FAB
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Fade-in animation for content
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, easing = EaseOutQuad),
        label = "alpha"
    )

    Surface(
        color = Color(0xFFFFFCF9),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            // Main content centered
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App title
                Text(
                    text = "Pebbl",
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF000000)
                    ),
                    modifier = Modifier.graphicsLayer { this.alpha = alpha }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Subtitle
                Text(
                    text = "Tap to start speaking",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color(0xFF2A2A37)
                    ),
                    modifier = Modifier.graphicsLayer { this.alpha = alpha }
                )
            }

            // FAB with breathing animation
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                // Main FAB
                FloatingActionButton(
                    onClick = onStart,
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center)
                        .scale(scale)
                        .graphicsLayer { this.alpha = alpha },
                    containerColor = Color(0xFFE9DED9),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 2.dp
                    ),
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Start voice recording",
                        tint = Color(0xFF000000),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

