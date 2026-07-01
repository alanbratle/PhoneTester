package com.phonetester.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestStatus
import com.phonetester.app.ui.theme.StatusFailed
import com.phonetester.app.ui.theme.StatusPassed
import com.phonetester.app.ui.theme.StatusPending
import com.phonetester.app.ui.theme.StatusSkipped
import com.phonetester.app.ui.theme.StatusTesting

@Composable
fun TestCard(
    category: TestCategory,
    status: TestStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            TestStatus.PASSED -> StatusPassed
            TestStatus.FAILED -> StatusFailed
            TestStatus.TESTING -> StatusTesting
            TestStatus.SKIPPED -> StatusSkipped
            TestStatus.PENDING -> StatusPending
        },
        animationSpec = tween(300),
        label = "statusColor"
    )

    ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                statusColor.copy(alpha = 0.2f),
                                statusColor.copy(alpha = 0.05f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.displayName.take(2),
                    style = MaterialTheme.typography.titleMedium,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StatusChip(status = status)
        }
    }
}

@Composable
fun StatusChip(status: TestStatus) {
    val text: String
    val color: Color
    when (status) {
        TestStatus.PASSED -> { text = "OK"; color = StatusPassed }
        TestStatus.FAILED -> { text = "FAIL"; color = StatusFailed }
        TestStatus.TESTING -> { text = "..."; color = StatusTesting }
        TestStatus.SKIPPED -> { text = "-"; color = StatusSkipped }
        TestStatus.PENDING -> { text = "?"; color = StatusPending }
    }
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    foregroundColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "progress"
    )
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val sweep = animatedProgress * 360f
            drawArc(
                color = backgroundColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = foregroundColor,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }
        Text(
            text = (animatedProgress * 100).toInt().toString() + "%",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    status: TestStatus = TestStatus.PASSED
) {
    val statusColor = when (status) {
        TestStatus.PASSED -> StatusPassed
        TestStatus.FAILED -> StatusFailed
        TestStatus.TESTING -> StatusTesting
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = statusColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreenScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = { actions() }
            )
        },
        content = content
    )
}