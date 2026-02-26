package com.mindaigle.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindaigle.ui.theme.CalmBlue
import com.mindaigle.ui.theme.StatusUrgent
import com.mindaigle.ui.theme.TextPrimary
import com.mindaigle.ui.theme.TextSecondary

/**
 * Shared empty state: icon, title, subtitle, optional primary action.
 * Use for lists/dashboards when there is no data.
 */
@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: androidx.compose.ui.graphics.Color = TextSecondary,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = iconTint
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            if (primaryActionLabel != null && onPrimaryAction != null) {
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
                ) {
                    Text(primaryActionLabel)
                }
            }
        }
    }
}

/**
 * Full-screen loading indicator: centered CalmBlue spinner.
 */
@Composable
fun LoadingFullScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = CalmBlue,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )
    }
}

/**
 * Error banner/card with message and optional retry.
 */
@Composable
fun ErrorBanner(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StatusUrgent.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = StatusUrgent
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = StatusUrgent,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                TextButton(onClick = onRetry) {
                    Text("Retry", color = CalmBlue)
                }
            }
        }
    }
}
