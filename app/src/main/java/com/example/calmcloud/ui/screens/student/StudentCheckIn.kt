package com.example.calmcloud.ui.screens.student

import androidx.compose.animation.core.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.calmcloud.data.remote.dto.FHIRObservation
import com.example.calmcloud.data.remote.dto.FHIRValueQuantity
import com.example.calmcloud.data.remote.dto.StudentFHIRData
import com.example.calmcloud.data.repository.StudentRepository
import com.example.calmcloud.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCheckIn(userName: String) {
    var selectedEmotion by remember { mutableStateOf<String?>(null) }
    var emotionIntensity by remember { mutableFloatStateOf(5f) }
    var stressLevel by remember { mutableFloatStateOf(5f) }
    var heartRate by remember { mutableStateOf("") }
    var waterIntake by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val repository = remember { StudentRepository() }

    val emotions = listOf(
        "happy" to "😊",
        "calm" to "😌",
        "okay" to "😐",
        "sad" to "😢",
        "anxious" to "😰",
        "stressed" to "😫"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Top Navigation
            TopAppBar(
                title = {
                    Text(
                        text = "Daily Check-in",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { /* Back */ }) {
                        Icon(Icons.Default.Menu, null)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Profile */ }) {
                        Icon(Icons.Default.Person, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Progress Dots (showing all 3 steps)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    (1..3).forEach { step ->
                        Box(
                            modifier = Modifier
                                .size(if (step == 3) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (step == 3) CalmBlue else Color.Gray.copy(alpha = 0.3f)
                                )
                        )
                        if (step < 3) {
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                }

                // Step 1: How are you feeling?
                EmotionSelectionCard(
                    emotions = emotions,
                    selectedEmotion = selectedEmotion,
                    onEmotionSelected = { selectedEmotion = it },
                    emotionIntensity = emotionIntensity,
                    onIntensityChange = { emotionIntensity = it }
                )

                // Step 2: Stress Level
                StressLevelCard(
                    stressLevel = stressLevel,
                    onStressChange = { stressLevel = it }
                )

                // Step 3: Health Metrics & Notes
                HealthMetricsCard(
                    heartRate = heartRate,
                    onHeartRateChange = { heartRate = it },
                    waterIntake = waterIntake,
                    onWaterIntakeChange = { waterIntake = it },
                    note = note,
                    onNoteChange = { note = it },
                    onSubmit = {
                        if (selectedEmotion == null) return@HealthMetricsCard
                        isLoading = true
                        scope.launch {
                            val observations = mutableListOf<FHIRObservation>()
                            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val timestamp = dateFormat.format(Date())

                            // Emotion
                            observations.add(
                                FHIRObservation(
                                    id = "emotion-${System.currentTimeMillis()}",
                                    code = com.example.calmcloud.data.remote.dto.FHIRCode(
                                        coding = listOf(
                                            com.example.calmcloud.data.remote.dto.FHIRCoding(
                                                code = "75258-2",
                                                display = "Emotional state"
                                            )
                                        )
                                    ),
                                    valueString = selectedEmotion,
                                    effectiveDateTime = timestamp,
                                    subject = com.example.calmcloud.data.remote.dto.FHIRSubject(
                                        reference = "Patient/1"
                                    )
                                )
                            )

                            // Stress
                            observations.add(
                                FHIRObservation(
                                    id = "stress-${System.currentTimeMillis()}",
                                    code = com.example.calmcloud.data.remote.dto.FHIRCode(
                                        coding = listOf(
                                            com.example.calmcloud.data.remote.dto.FHIRCoding(
                                                code = "73985-4",
                                                display = "Stress level"
                                            )
                                        )
                                    ),
                                    valueQuantity = FHIRValueQuantity(
                                        value = stressLevel.toDouble(),
                                        unit = "1-10 scale"
                                    ),
                                    effectiveDateTime = timestamp,
                                    subject = com.example.calmcloud.data.remote.dto.FHIRSubject(
                                        reference = "Patient/1"
                                    )
                                )
                            )

                            // Heart Rate
                            heartRate.toIntOrNull()?.let {
                                observations.add(
                                    FHIRObservation(
                                        id = "heartRate-${System.currentTimeMillis()}",
                                        code = com.example.calmcloud.data.remote.dto.FHIRCode(
                                            coding = listOf(
                                                com.example.calmcloud.data.remote.dto.FHIRCoding(
                                                    code = "8867-4",
                                                    display = "Heart rate"
                                                )
                                            )
                                        ),
                                        valueQuantity = FHIRValueQuantity(
                                            value = it.toDouble(),
                                            unit = "BPM"
                                        ),
                                        effectiveDateTime = timestamp,
                                        subject = com.example.calmcloud.data.remote.dto.FHIRSubject(
                                            reference = "Patient/1"
                                        )
                                    )
                                )
                            }

                            // Water
                            waterIntake.toIntOrNull()?.let {
                                observations.add(
                                    FHIRObservation(
                                        id = "hydration-${System.currentTimeMillis()}",
                                        code = com.example.calmcloud.data.remote.dto.FHIRCode(
                                            coding = listOf(
                                                com.example.calmcloud.data.remote.dto.FHIRCoding(
                                                    code = "9052-2",
                                                    display = "Fluid intake"
                                                )
                                            )
                                        ),
                                        valueQuantity = FHIRValueQuantity(
                                            value = it.toDouble(),
                                            unit = "mL"
                                        ),
                                        effectiveDateTime = timestamp,
                                        subject = com.example.calmcloud.data.remote.dto.FHIRSubject(
                                            reference = "Patient/1"
                                        )
                                    )
                                )
                            }

                            repository.saveStudentData(
                                studentId = null,
                                fhirData = StudentFHIRData(observations = observations)
                            )
                                .onSuccess {
                                    showSuccess = true
                                    isLoading = false
                                    // Reset form after delay
                                    kotlinx.coroutines.delay(2000)
                                    selectedEmotion = null
                                    emotionIntensity = 5f
                                    stressLevel = 5f
                                    heartRate = ""
                                    waterIntake = ""
                                    note = ""
                                    showSuccess = false
                                }
                                .onFailure {
                                    isLoading = false
                                }
                        }
                    },
                    isLoading = isLoading,
                    canSubmit = selectedEmotion != null
                )

                // Success Message
                if (showSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = CalmGreen.copy(alpha = 0.15f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = CalmGreen, modifier = Modifier.size(32.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Check-in saved successfully! 🎉",
                                    color = CalmGreen,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Great job taking care of yourself!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }

                // Footer
                Text(
                    text = "Your data is stored securely in FHIR format and only shared with authorized care team members",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun EmotionSelectionCard(
    emotions: List<Pair<String, String>>,
    selectedEmotion: String?,
    onEmotionSelected: (String) -> Unit,
    emotionIntensity: Float,
    onIntensityChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StatusUrgent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Favorite, null, tint = StatusUrgent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "How are you feeling?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Select your current mood",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 2x3 Emotion Grid
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    emotions.take(3).forEach { (emotion, emoji) ->
                        EmotionButton(
                            emotion = emotion,
                            emoji = emoji,
                            selected = selectedEmotion == emotion,
                            onClick = { onEmotionSelected(emotion) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    emotions.drop(3).forEach { (emotion, emoji) ->
                        EmotionButton(
                            emotion = emotion,
                            emoji = emoji,
                            selected = selectedEmotion == emotion,
                            onClick = { onEmotionSelected(emotion) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Intensity Slider
            if (selectedEmotion != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Intensity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${emotionIntensity.toInt()}/10",
                            style = MaterialTheme.typography.titleLarge,
                            color = CalmBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Slider(
                        value = emotionIntensity,
                        onValueChange = onIntensityChange,
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = CalmBlue,
                            activeTrackColor = CalmBlue,
                            inactiveTrackColor = CalmBlue.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmotionButton(
    emotion: String,
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "emotion_scale"
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) CalmBlue.copy(alpha = 0.2f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 8.dp else 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.displayMedium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = emotion.replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
fun StressLevelCard(
    stressLevel: Float,
    onStressChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Brain Icon (using emoji as Material Icons doesn't have brain) - Pink theme
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StatusUrgent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧠", style = MaterialTheme.typography.titleLarge)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Stress Level",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Rate your stress: ${stressLevel.toInt()}/10",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Slider
            Slider(
                value = stressLevel,
                onValueChange = onStressChange,
                valueRange = 1f..10f,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = CalmBlue,
                    activeTrackColor = CalmBlue,
                    inactiveTrackColor = CalmPurple.copy(alpha = 0.5f)
                ),
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("😌", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Relaxed",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("😩", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Very Stressed",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun HealthMetricsCard(
    heartRate: String,
    onHeartRateChange: (String) -> Unit,
    waterIntake: String,
    onWaterIntakeChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean,
    canSubmit: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Health Metrics
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Health Metrics (Optional)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(StatusUrgent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, null, tint = StatusUrgent, modifier = Modifier.size(20.dp))
                    }
                    OutlinedTextField(
                        value = heartRate,
                        onValueChange = onHeartRateChange,
                        label = { Text("Heart Rate (bpm)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g., 75") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(CalmBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Favorite, null, tint = CalmBlue, modifier = Modifier.size(20.dp))
                    }
                    OutlinedTextField(
                        value = waterIntake,
                        onValueChange = onWaterIntakeChange,
                        label = { Text("Water Intake (mL)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("e.g., 500") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceLight,
                            unfocusedContainerColor = SurfaceLight
                        )
                    )
                }
            }
        }

        // Additional Notes
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Additional Notes (Optional)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("How was your day? Any thoughts or concerns...") },
                    minLines = 3,
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceLight,
                        unfocusedContainerColor = SurfaceLight
                    )
                )
            }
        }

        // Submit Button with Blue Gradient (Midnaigle theme)
        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && canSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(CalmBlue, CalmBlueDark)
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Submit Check-in",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
