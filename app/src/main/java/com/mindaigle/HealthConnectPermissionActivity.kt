package com.mindaigle

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.activity.compose.setContent
import androidx.compose.ui.unit.dp
import com.mindaigle.data.health.HealthConnectManager
import com.mindaigle.ui.theme.MindAigleTheme

/**
 * Activity that requests Health Connect permissions. Shows a clear UI (no white screen).
 * If the permission screen doesn't open, user can tap "Open Health Connect" and go back.
 */
class HealthConnectPermissionActivity : ComponentActivity() {

    private val permissionContract = HealthConnectManager.createPermissionContract()
    private val launcher = registerForActivityResult(permissionContract) { granted ->
        if (!granted.isNullOrEmpty()) {
            setResult(RESULT_OK)
            Toast.makeText(this, "Access granted. Returning to refresh data.", Toast.LENGTH_SHORT).show()
        } else {
            setResult(RESULT_CANCELED)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MindAigleTheme {
                PermissionRequestScreen(
                    onOpenHealthConnect = {
                        try {
                            val intent = HealthConnectManager.getHealthConnectIntent(this).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        } catch (_: Throwable) { }
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onBack = {
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                )
            }
        }
        val permissions = HealthConnectManager.getRequiredPermissionStrings()
        if (permissions.isEmpty()) {
            setResult(RESULT_CANCELED)
            finish()
            return
        }
        try {
            launcher.launch(permissions)
        } catch (_: Throwable) {
            Toast.makeText(this, "Use \"Open Health Connect\" below, then add MindAigle in Steps → Access.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun PermissionRequestScreen(
    onOpenHealthConnect: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Requesting Health Connect permission…",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "If the permission screen didn't open, tap below to open Health Connect. Then go to Steps → Access and add MindAigle.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenHealthConnect, modifier = Modifier.fillMaxWidth()) {
            Text("Open Health Connect app")
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Back")
        }
    }
}
