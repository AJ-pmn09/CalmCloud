package com.mindaigle.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mindaigle.ui.theme.*
import kotlin.math.max
import kotlin.math.min

/**
 * Line chart for displaying trends over time
 */
@Composable
fun LineChart(
    data: List<Float>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    color: Color = CalmBlue,
    showGrid: Boolean = true,
    showPoints: Boolean = true,
    showArea: Boolean = false
) {
    if (data.isEmpty()) return

    val animatedProgress = remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        animatedProgress.value = 1f
    }
    val progress by animateFloatAsState(
        targetValue = animatedProgress.value,
        animationSpec = tween(1000),
        label = "chart_animation"
    )

    val maxValue = data.maxOrNull() ?: 10f
    val minValue = data.minOrNull() ?: 0f
    val range = (maxValue - minValue).coerceAtLeast(1f)
    val normalizedData = data.map { (it - minValue) / range }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 40.dp.toPx()
            val chartWidth = width - padding * 2
            val chartHeight = height - padding * 2

            if (showGrid) {
                val gridColor = Color.LightGray.copy(alpha = 0.25f)
                for (i in 0..4) {
                    val y = padding + (chartHeight / 4) * i
                    drawLine(
                        color = gridColor,
                        start = Offset(padding, y),
                        end = Offset(width - padding, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            if (normalizedData.size > 1) {
                val linePath = Path()
                val pointSpacing = chartWidth / (normalizedData.size - 1)

                normalizedData.forEachIndexed { index, value ->
                    val x = padding + pointSpacing * index
                    val y = padding + chartHeight - (value * chartHeight * progress)
                    val point = Offset(x, y)
                    if (index == 0) linePath.moveTo(point.x, point.y)
                    else linePath.lineTo(point.x, point.y)
                }

                if (showArea) {
                    val areaPath = Path().apply {
                        addPath(linePath)
                        lineTo(padding + chartWidth, padding + chartHeight)
                        lineTo(padding, padding + chartHeight)
                        close()
                    }
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                color.copy(alpha = 0.25f),
                                color.copy(alpha = 0.04f)
                            ),
                            startY = padding,
                            endY = padding + chartHeight
                        )
                    )
                }

                drawPath(
                    path = linePath,
                    color = color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                if (showPoints) {
                    normalizedData.forEachIndexed { index, value ->
                        val x = padding + pointSpacing * index
                        val y = padding + chartHeight - (value * chartHeight * progress)
                        drawCircle(
                            color = color,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Multi-series line chart: one line per student (or series) with legend.
 * Use for peer comparison (stress by student, activity by student).
 */
@Composable
fun MultiSeriesLineChart(
    series: List<Pair<String, List<Float>>>,
    labels: List<String> = emptyList(),
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(StatusUrgent, CalmBlue, CalmGreen, Color(0xFFFF9800), Color(0xFF9C27B0)),
    showGrid: Boolean = true,
    showPoints: Boolean = true
) {
    if (series.isEmpty()) return
    val animatedProgress = remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        animatedProgress.value = 1f
    }
    val progress by animateFloatAsState(
        targetValue = animatedProgress.value,
        animationSpec = tween(1000),
        label = "multichart_animation"
    )
    val allValues = series.flatMap { it.second }
    val maxValue = allValues.maxOrNull() ?: 10f
    val minValue = allValues.minOrNull() ?: 0f
    val range = (maxValue - minValue).coerceAtLeast(1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(SurfaceCard, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val padding = 40.dp.toPx()
                    val chartWidth = width - padding * 2
                    val chartHeight = height - padding * 2

                    if (showGrid) {
                        val gridColor = Color.LightGray.copy(alpha = 0.3f)
                        for (i in 0..4) {
                            val y = padding + (chartHeight / 4) * i
                            drawLine(
                                color = gridColor,
                                start = Offset(padding, y),
                                end = Offset(width - padding, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                    }

                    series.forEachIndexed { seriesIndex, (_, data) ->
                        if (data.isEmpty()) return@forEachIndexed
                        val normalizedData = data.map { (it - minValue) / range }
                        val color = colors[seriesIndex % colors.size]
                        if (normalizedData.size > 1) {
                            val path = Path()
                            val pointSpacing = chartWidth / (normalizedData.size - 1).coerceAtLeast(1)
                            normalizedData.forEachIndexed { index, value ->
                                val x = padding + pointSpacing * index
                                val y = padding + chartHeight - (value * chartHeight * progress)
                                val point = Offset(x, y)
                                if (index == 0) path.moveTo(point.x, point.y)
                                else path.lineTo(point.x, point.y)
                            }
                            drawPath(
                                path = path,
                                color = color,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                            if (showPoints) {
                                normalizedData.forEachIndexed { index, value ->
                                    val x = padding + pointSpacing * index
                                    val y = padding + chartHeight - (value * chartHeight * progress)
                                    drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                                    drawCircle(color = Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
                                }
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                series.forEachIndexed { index, (label, _) ->
                    val color = colors[index % colors.size]
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(color, RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = label.take(12).let { if (label.length > 12) "$it…" else it },
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Pie chart for emotion distribution
 */
@Composable
fun PieChart(
    data: Map<String, Float>,
    modifier: Modifier = Modifier,
    colors: Map<String, Color> = emptyMap()
) {
    val total = data.values.sum()
    if (total == 0f) return
    
    val defaultColors = mapOf(
        "happy" to EmotionHappy,
        "calm" to EmotionCalm,
        "okay" to EmotionOkay,
        "sad" to EmotionSad,
        "anxious" to EmotionAnxious,
        "stressed" to EmotionStressed
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = min(size.width, size.height) / 2 - 20.dp.toPx()
            var startAngle = -90f
            
            data.forEach { (key, value) ->
                val sweepAngle = (value / total) * 360f
                val color = colors[key] ?: defaultColors[key] ?: CalmBlue
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * 0.95f, // Small gap between segments
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                
                startAngle += sweepAngle
            }
        }
        
        // Legend
        Column(
            modifier = Modifier.align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            data.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                colors[key] ?: defaultColors[key] ?: CalmBlue,
                                RoundedCornerShape(2.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${key.replaceFirstChar { it.uppercaseChar() }}: ${(value / total * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

/**
 * Bar chart for comparing values
 */
@Composable
fun BarChart(
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    color: Color = CalmBlue,
    maxValue: Float? = null
) {
    if (data.isEmpty()) return
    
    val animatedProgress = remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        animatedProgress.value = 1f
    }
    val progress by animateFloatAsState(
        targetValue = animatedProgress.value,
        animationSpec = tween(800),
        label = "bar_animation"
    )
    
    val max = maxValue ?: data.maxOfOrNull { it.second } ?: 10f
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(SurfaceCard, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val barCount = data.size
            val barSpacing = 8.dp.toPx()
            val availableWidth = width - (barSpacing * (barCount - 1))
            val barWidth = availableWidth / barCount
            val chartHeight = height - 40.dp.toPx()
            
            data.forEachIndexed { index, (label, value) ->
                val barHeight = (value / max) * chartHeight * progress
                val x = (barWidth + barSpacing) * index
                val y = height - barHeight - 20.dp.toPx()
                
                // Draw bar with gradient
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            color,
                            color.copy(alpha = 0.7f)
                        )
                    ),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}

/**
 * Progress ring for displaying single metric
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = CalmBlue,
    backgroundColor: Color = SurfaceLight,
    strokeWidth: Float = 12f,
    text: String? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000),
        label = "ring_animation"
    )
    
    Box(
        modifier = modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = (size.minDimension - strokeWidth.dp.toPx()) / 2
            
            // Background circle
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth.dp.toPx())
            )
            
            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth.dp.toPx(), cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }
        
        text?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

