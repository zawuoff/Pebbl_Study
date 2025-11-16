package com.fouwaz.studypal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fouwaz.studypal.data.local.entity.AchievementEntity
import com.fouwaz.studypal.domain.model.PebbleTypes

@Composable
fun RecentPebblesRow(
    achievements: List<AchievementEntity>,
    onViewCollection: () -> Unit
) {
    if (achievements.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header with "View Collection" link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Pebbles",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = Color(0xFF2A2A37).copy(alpha = 0.7f)
            )

            TextButton(
                onClick = onViewCollection,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "View Collection",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF000000)
                )
            }
        }

        // Pebbles row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            achievements.take(5).forEach { achievement ->
                val pebble = PebbleTypes.getPebbleById(achievement.pebbleType)

                pebble?.let {
                    PebbleChip(
                        pebbleType = it,
                        isNew = achievement.isNew
                    )
                }
            }
        }
    }
}

@Composable
private fun PebbleChip(
    pebbleType: com.fouwaz.studypal.domain.model.PebbleType,
    isNew: Boolean
) {
    Box {
        Surface(
            shape = CircleShape,
            color = pebbleType.color.copy(alpha = 0.3f),
            shadowElevation = 2.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                // Placeholder pebble icon
                Text(
                    text = "🪨",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        // "New" badge
        if (isNew) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE9DED9),
                shadowElevation = 1.dp,
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color(0xFF000000)
                    )
                }
            }
        }
    }
}
