package com.mindaigle.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindaigle.ui.theme.*

/**
 * Onboarding that explains how wearable data reaches the app.
 * Armitron Connect (and most consumer wearables) do NOT expose a direct API.
 * Path: Watch → Companion app (e.g. Armitron Connect) → Health Connect → MindAigle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthOnboardingScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(BackgroundLight)
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Icon(
            Icons.Default.Info,
            contentDescription = "How wearable data works",
            modifier = Modifier.size(56.dp),
            tint = CalmBlue
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "How wearable data works",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Your watch syncs to its companion app, which writes data to your phone's health platform. MindAigle then reads that data with your permission.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Works with Samsung, Fitbit, Garmin, Armitron, and any device that syncs to Health Connect.",
            style = MaterialTheme.typography.bodySmall,
            color = com.mindaigle.ui.theme.CalmBlue
        )
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = com.mindaigle.ui.theme.SurfaceLight),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "We use steps, sleep, and heart rate to show your progress and link activity to wellbeing. You can revoke access anytime in Health Connect.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(12.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp)) {
                StepRow(1, "Sync your watch", "Open the Armitron Connect app (or your wearable's app) and sync your device.")
                Spacer(Modifier.height(12.dp))
                StepRow(2, "Data goes to Health Connect", "The companion app writes steps, sleep, heart rate, etc. to Android's Health Connect.")
                Spacer(Modifier.height(12.dp))
                StepRow(3, "MindAigle reads with your permission", "We ask for access to read that data so we can show it here. We never connect directly to your watch.")
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "You must have synced your watch at least once in the companion app for data to appear here.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = CalmBlue)
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun StepRow(step: Int, title: String, body: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = CalmBlue.copy(alpha = 0.15f)
        ) {
            Text(
                "$step",
                style = MaterialTheme.typography.titleMedium,
                color = CalmBlue,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}
