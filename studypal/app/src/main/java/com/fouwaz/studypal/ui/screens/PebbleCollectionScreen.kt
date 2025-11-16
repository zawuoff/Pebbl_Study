package com.fouwaz.studypal.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fouwaz.studypal.VoiceStreamApplication
import com.fouwaz.studypal.domain.model.PebbleRarity
import com.fouwaz.studypal.domain.model.PebbleType
import com.fouwaz.studypal.domain.model.PebbleTypes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PebbleCollectionScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val database = (context.applicationContext as VoiceStreamApplication).database
    val achievementDao = database.achievementDao()

    var unlockedPebbleIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isVisible by remember { mutableStateOf(false) }

    // Load unlocked achievements
    LaunchedEffect(Unit) {
        val achievements = achievementDao.getAllAchievementsSync()
        unlockedPebbleIds = achievements.map { it.pebbleType }.toSet()
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
                            text = "Pebble Collection",
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
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                // Header text
                Text(
                    text = "Collect pebbles by reaching word milestones",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF2A2A37).copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Pebble grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(PebbleTypes.ALL_PEBBLES) { pebble ->
                        val isUnlocked = unlockedPebbleIds.contains(pebble.id)
                        PebbleCard(
                            pebble = pebble,
                            isUnlocked = isUnlocked
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PebbleCard(
    pebble: PebbleType,
    isUnlocked: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isUnlocked) Color(0xFFF8F8F8) else Color(0xFFF8F8F8).copy(alpha = 0.5f),
        shadowElevation = if (isUnlocked) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Rarity indicator
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = getRarityColor(pebble.rarity).copy(alpha = 0.2f)
            ) {
                Text(
                    text = pebble.rarity.name,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (isUnlocked) getRarityColor(pebble.rarity) else Color(0xFF2A2A37).copy(alpha = 0.3f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Pebble illustration
            if (isUnlocked) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(pebble.color.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🪨",
                        style = MaterialTheme.typography.displaySmall
                    )
                }
            } else {
                // Locked silhouette
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFF2A2A37).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🔒",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            // Pebble info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (isUnlocked) pebble.name else "???",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isUnlocked) Color(0xFF000000) else Color(0xFF2A2A37).copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Text(
                    text = if (isUnlocked)
                        pebble.description
                    else
                        "${pebble.wordMilestone} words to unlock",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2A2A37).copy(alpha = if (isUnlocked) 0.6f else 0.4f),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }
    }
}

private fun getRarityColor(rarity: PebbleRarity): Color {
    return when (rarity) {
        PebbleRarity.COMMON -> Color(0xFF9E9E9E)
        PebbleRarity.UNCOMMON -> Color(0xFF4CAF50)
        PebbleRarity.RARE -> Color(0xFF2196F3)
        PebbleRarity.EPIC -> Color(0xFF9C27B0)
        PebbleRarity.LEGENDARY -> Color(0xFFFFB300)
    }
}
