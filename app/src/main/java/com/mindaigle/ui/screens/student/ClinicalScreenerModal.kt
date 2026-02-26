package com.mindaigle.ui.screens.student

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindaigle.data.remote.dto.ScreenerCatalogItem
import com.mindaigle.data.remote.dto.ScreenerInstance
import com.mindaigle.data.repository.ScreenerRepository
import com.mindaigle.ui.theme.*
import kotlinx.coroutines.launch

private val ANSWER_LABELS = listOf("Not at all", "Several days", "More than half the days", "Nearly every day")

private fun displayNameForType(type: String): String {
    val t = type.trim().lowercase()
    return when (t) {
        "phq9" -> "PHQ-9"
        "gad7" -> "GAD-7"
        else -> type.uppercase()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClinicalScreenerModal(
    instance: ScreenerInstance,
    catalogItem: ScreenerCatalogItem?,
    studentId: Int,
    onDismiss: () -> Unit,
    onCompleted: () -> Unit
) {
    val repo = remember { ScreenerRepository() }
    val scope = rememberCoroutineScope()
    // Resolve questionnaire from the assigned screener type (staff-assigned PHQ-9 or GAD-7)
    var resolvedCatalogItem by remember(instance.id, instance.screenerType) { mutableStateOf(catalogItem) }
    LaunchedEffect(instance.screenerType, catalogItem) {
        resolvedCatalogItem = catalogItem
        if (resolvedCatalogItem == null) {
            repo.getCatalog().onSuccess { list ->
                resolvedCatalogItem = list.find {
                    it.screenerType.trim().equals(instance.screenerType.trim(), ignoreCase = true)
                }
            }
        }
    }
    val questions = resolvedCatalogItem?.questions ?: emptyList()
    val answers = remember(questions.size) {
        mutableStateListOf<Int>().apply { repeat(questions.size) { add(-1) } }
    }
    LaunchedEffect(questions.size) {
        if (answers.size != questions.size) {
            answers.clear()
            repeat(questions.size) { answers.add(-1) }
        }
    }
    var currentStep by remember { mutableIntStateOf(0) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var submitResult by remember { mutableStateOf<Pair<Int, String>?>(null) }

    LaunchedEffect(instance.id, resolvedCatalogItem) {
        if (questions.isEmpty() && resolvedCatalogItem == null) errorMessage = "Loading ${instance.screenerType.uppercase()} questions…"
        else if (questions.isEmpty()) errorMessage = "Questions not loaded"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = resolvedCatalogItem?.name ?: displayNameForType(instance.screenerType),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                errorMessage?.let { msg ->
                    Text(text = msg, color = MaterialTheme.colorScheme.error)
                }
                when {
                    submitResult != null -> {
                        val (total, severity) = submitResult!!
                        Text(
                            text = "Thank you for completing this screener.",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Your responses have been saved. A staff member may follow up if needed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (total >= 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CalmBlue.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Score: $total",
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "•",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = severity,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    questions.isNotEmpty() -> {
                        Text(
                            text = "Over the last 2 weeks, how often have you been bothered by the following?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val qIndex = currentStep.coerceIn(0, questions.size - 1)
                        val question = questions[qIndex]
                        Text(
                            text = question,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium
                        )
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ANSWER_LABELS.forEachIndexed { index, label ->
                                FilterChip(
                                    selected = answers.getOrNull(qIndex) == index,
                                    onClick = {
                                        if (qIndex in answers.indices) {
                                            answers[qIndex] = index
                                        }
                                    },
                                    label = { Text(text = label, maxLines = 1) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CalmBlue.copy(alpha = 0.2f)
                                    )
                                )
                            }
                        }
                        if (questions.size > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Question ${qIndex + 1} of ${questions.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                submitResult != null -> {
                    Button(onClick = {
                        onCompleted()
                        onDismiss()
                    }) {
                        Text("Done")
                    }
                }
                questions.isEmpty() -> {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                else -> {
                    val allAnswered = (0 until questions.size).all { answers.getOrNull(it) in 0..3 }
                    val isLast = currentStep >= questions.size - 1
                    if (isLast) {
                        Button(
                            onClick = {
                                if (!allAnswered) return@Button
                                isSubmitting = true
                                errorMessage = null
                                scope.launch {
                                    val result = repo.submitScreener(
                                        instanceId = instance.id,
                                        studentId = studentId,
                                        answers = answers.map { it.coerceIn(0, 3) }
                                    )
                                    isSubmitting = false
                                    result.onSuccess { data ->
                                        submitResult = data.total to data.severity
                                    }.onFailure {
                                        errorMessage = it.message ?: "Submit failed"
                                    }
                                }
                            },
                            enabled = allAnswered && !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Submit")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (currentStep < questions.size - 1) currentStep++
                            },
                            enabled = answers.getOrNull(currentStep) in 0..3
                        ) {
                            Text("Next")
                        }
                    }
                }
            }
        },
        dismissButton = {
            if (submitResult == null && questions.isNotEmpty() && currentStep > 0) {
                TextButton(onClick = { currentStep = (currentStep - 1).coerceAtLeast(0) }) {
                    Text("Back")
                }
            } else if (submitResult == null) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
