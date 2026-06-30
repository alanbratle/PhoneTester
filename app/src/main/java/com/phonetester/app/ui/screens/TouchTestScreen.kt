package com.phonetester.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestDetail
import com.phonetester.app.model.TestResult
import com.phonetester.app.model.TestStatus
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel

private val touchColors = listOf(
    Color(0xFFFF4444),
    Color(0xFF44FF44),
    Color(0xFF4488FF),
    Color(0xFFFFAA00),
    Color(0xFFFF44FF),
    Color(0xFF44FFFF),
    Color(0xFFFFFF44),
    Color(0xFFFF8844),
    Color(0xFF8844FF),
    Color(0xFF44FFAA)
)

private data class TouchPoint(
    val id: Int,
    val x: Float,
    val y: Float
)

@Composable
fun TouchTestScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val activePoints = remember { mutableStateListOf<TouchPoint>() }
    var maxSimultaneousPoints by remember { mutableIntStateOf(0) }

    fun finishTest() {
        val passed = maxSimultaneousPoints >= 5
        val status = if (passed) TestStatus.PASSED else TestStatus.FAILED

        val details = buildList {
            add(
                TestDetail(
                    "Мультитач",
                    "Макс. одновременных точек: $maxSimultaneousPoints",
                    if (passed) TestStatus.PASSED else TestStatus.FAILED
                )
            )
            if (!passed) {
                add(
                    TestDetail(
                        "Рекомендация",
                        "Минимум 5 точек рекомендуется для нормальной работы",
                        TestStatus.FAILED
                    )
                )
            }
        }

        viewModel.updateResult(
            TestCategory.TOUCH,
            TestResult(
                category = TestCategory.TOUCH,
                status = status,
                details = details
            )
        )
        onBack()
    }

    BackHandler {
        finishTest()
    }

    TestScreenScaffold(
        title = "Тест тачскрина",
        onBack = { finishTest() }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val type = event.type

                            val pressedChanges = event.changes.filter { it.pressed }
                            val releasedChanges = event.changes.filter { !it.pressed }

                            val pressedIds = pressedChanges.map { it.id.value }.toSet()
                            val releasedIds = releasedChanges.map { it.id.value }.toSet()

                            // Update max simultaneous points
                            if (pressedChanges.size > maxSimultaneousPoints) {
                                maxSimultaneousPoints = pressedChanges.size
                            }

                            // Remove released pointers
                            if (releasedIds.isNotEmpty()) {
                                val iterator = activePoints.iterator()
                                while (iterator.hasNext()) {
                                    if (iterator.next().id in releasedIds) {
                                        iterator.remove()
                                    }
                                }
                            }

                            // Add or update pressed pointers
                            for (change in pressedChanges) {
                                val id = change.id.value
                                val idx = activePoints.indexOfFirst { it.id == id }
                                val newPoint = TouchPoint(
                                    id = id,
                                    x = change.position.x,
                                    y = change.position.y
                                )
                                if (idx >= 0) {
                                    activePoints[idx] = newPoint
                                } else {
                                    activePoints.add(newPoint)
                                }
                            }

                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            // Canvas overlay to draw touch circles at exact pixel positions
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Pass-through: consume nothing, let parent handle touches
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
            ) {
                val points = activePoints.toList()
                for (point in points) {
                    val color = touchColors[point.id % touchColors.size]
                    val center = Offset(point.x, point.y)

                    // Outer semi-transparent filled circle
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = 50f,
                        center = center
                    )
                    // Inner more opaque circle
                    drawCircle(
                        color = color.copy(alpha = 0.6f),
                        radius = 30f,
                        center = center
                    )
                    // Center dot
                    drawCircle(
                        color = color.copy(alpha = 0.9f),
                        radius = 8f,
                        center = center
                    )
                    // Outer ring
                    drawCircle(
                        color = color.copy(alpha = 0.5f),
                        radius = 50f,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
            }

            // Top instruction
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Рисуйте пальцами по экрану",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Максимум точек: $maxSimultaneousPoints",
                    color = if (maxSimultaneousPoints >= 5) Color(0xFF44FF44)
                    else Color(0xFFFFAA00),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
                if (maxSimultaneousPoints < 5) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Коснитесь экрана 5+ пальцами одновременно",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom finish button
            Button(
                onClick = { finishTest() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Завершить",
                    fontSize = 16.sp
                )
            }
        }
    }
}