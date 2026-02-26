package com.mindaigle.ui.components

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindaigle.ui.theme.*

data class EmotionOption(
    val value: String,
    val emoji: String,
    val label: String,
    val color: Color
)

@Composable
fun EnhancedEmotionPicker(
    emotions: List<EmotionOption>,
    selectedEmotion: String?,
    onEmotionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "How are you feeling?",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Tap an emotion to express yourself",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )

        // Enhanced Emotion Grid with Better Spacing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            emotions.chunked(3).forEach { rowEmotions ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowEmotions.forEach { emotion ->
                        EmotionCard(
                            emotion = emotion,
                            selected = selectedEmotion == emotion.value,
                            onClick = { onEmotionSelected(emotion.value) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmotionCard(
    emotion: EmotionOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "emotion_scale"
    )
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .scale(scale),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) emotion.color else SurfaceLight
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 12.dp else 2.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = emotion.emoji,
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = emotion.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

