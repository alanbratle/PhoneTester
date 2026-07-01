package com.phonetester.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestDetail
import com.phonetester.app.model.TestResult
import com.phonetester.app.model.TestStatus
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.ui.theme.ScreenTestBlack
import com.phonetester.app.ui.theme.ScreenTestBlue
import com.phonetester.app.ui.theme.ScreenTestGreen
import com.phonetester.app.ui.theme.ScreenTestRed
import com.phonetester.app.ui.theme.ScreenTestWhite
import com.phonetester.app.viewmodel.DashboardViewModel

private val solidColors = listOf(
    ScreenTestRed,
    ScreenTestGreen,
    ScreenTestBlue,
    ScreenTestWhite,
    ScreenTestBlack
)

private data class GradientPattern(
    val name: String,
    val brushProvider: (Float, Float) -> Brush
)

private val gradientPatterns = listOf(
    GradientPattern("Горизонтальный градиент RGB") { _, maxY ->
        Brush.horizontalGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green,
                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
        )
    },
    GradientPattern("Вертикальный градиент RGB") { _, _ ->
        Brush.verticalGradient(
            colors = listOf(
                Color.Red, Color.Yellow, Color.Green,
                Color.Cyan, Color.Blue, Color.Magenta, Color.Red
            )
        )
    },
    GradientPattern("Радиальный градиент") { _, _ ->
        Brush.radialGradient(
            colors = listOf(Color.White, Color.Gray, Color.Black)
        )
    },
    GradientPattern("Чёрно-белые полосы") { _, _ ->
        Brush.horizontalGradient(
            colors = buildList {
                repeat(16) { i ->
                    add(if (i % 2 == 0) Color.Black else Color.White)
                }
            }
        )
    }
)

private val totalSteps = solidColors.size + gradientPatterns.size

@Composable
fun ScreenTestScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }

    fun finishTest(status: TestStatus = TestStatus.PASSED) {
        val details = buildList {
            add(TestDetail("Цвета экрана", "Все 5 цветов проверены", status))
            add(
                TestDetail(
                    "Битые пиксели",
                    if (status == TestStatus.PASSED) "Не обнаружены" else "Проверка прервана",
                    status
                )
            )
        }
        viewModel.updateResult(
            TestCategory.SCREEN,
            TestResult(
                category = TestCategory.SCREEN,
                status = status,
                details = details
            )
        )
        onBack()
    }

    BackHandler {
        if (step == 0) {
            finishTest(TestStatus.SKIPPED)
        } else {
            finishTest(TestStatus.PASSED)
        }
    }

    TestScreenScaffold(
        title = "Тест экрана (${step + 1}/$totalSteps)",
        onBack = { finishTest(TestStatus.SKIPPED) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(getCurrentBackground(step))
                .pointerInput(step) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.pressed }) {
                                event.changes.forEach { it.consume() }
                                if (step < totalSteps - 1) {
                                    step++
                                } else {
                                    finishTest(TestStatus.PASSED)
                                    break
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            val hintText = if (step < solidColors.size) {
                "Нажмите для смены цвета"
            } else {
                "Нажмите для следующего паттерна"
            }

            val textColor = when {
                step < solidColors.size && solidColors[step] == ScreenTestBlack -> Color.White
                step < solidColors.size && solidColors[step] == ScreenTestWhite -> Color.Black
                step >= solidColors.size -> {
                    val patternIndex = step - solidColors.size
                    if (patternIndex == 2) Color.White else Color.Black
                }
                else -> Color.White
            }

            Text(
                text = if (step < solidColors.size) hintText else "Проверка на битые пиксели\n$hintText",
                color = textColor.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )
        }
    }

}

@Composable
private fun getCurrentBackground(step: Int): Brush {
    return if (step < solidColors.size) {
        SolidColor(solidColors[step])
    } else {
        val patternIndex = step - solidColors.size
        val pattern = gradientPatterns.getOrElse(patternIndex) {
            return SolidColor(Color.Black)
        }
        pattern.brushProvider(0f, 0f)
    }
}