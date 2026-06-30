package com.phonetester.app.ui.screens

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phonetester.app.model.*
import com.phonetester.app.ui.components.DetailRow
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel

@Composable
fun DisplayInfoScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val display: Display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display ?: windowManager.defaultDisplay
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
    }

    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    display.getMetrics(metrics)

    val realMetrics = DisplayMetrics()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
        display.getRealMetrics(realMetrics)
    } else {
        @Suppress("DEPRECATION")
        display.getMetrics(realMetrics)
    }

    val resolutionWidth = realMetrics.widthPixels
    val resolutionHeight = realMetrics.heightPixels

    val densityDpi = realMetrics.densityDpi
    val density = realMetrics.density

    val refreshRate: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display.mode?.refreshRate ?: display.refreshRate
    } else {
        @Suppress("DEPRECATION")
        display.refreshRate
    }

    // Calculate physical screen size in inches
    val screenWidthInches = realMetrics.widthPixels / realMetrics.xdpi
    val screenHeightInches = realMetrics.heightPixels / realMetrics.ydpi
    val screenDiagonalInches = kotlin.math.sqrt(
        screenWidthInches * screenWidthInches + screenHeightInches * screenHeightInches
    )

    // HDR support
    val isHdrSupported: Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        display.hdrCapabilities?.let { caps ->
            caps.supportedHdrTypes?.isNotEmpty() == true
        } ?: false
    } else {
        false
    }

    val hdrType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        display.hdrCapabilities?.supportedHdrTypes?.mapNotNull { type ->
            when (type) {
                Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                else -> null
            }
        }?.joinToString(", ") ?: "Не поддерживается"
    } else {
        "Не поддерживается (API < 26)"
    }

    // Color mode
    val colorMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        @Suppress("DEPRECATION")
        when (display.mode?.colorMode) {
            Display.COLOR_MODE_DEFAULT -> "Стандартный (sRGB)"
            Display.COLOR_MODE_WIDE_COLOR_GAMUT -> "Широкая цветовая гамма"
            else -> "Неизвестно"
        }
    } else {
        "Стандартный (API < 26)"
    }

    val details = listOf(
        TestDetail("Разрешение", "${resolutionWidth} × ${resolutionHeight} пикс.", TestStatus.PASSED),
        TestDetail("Плотность пикселей", "${densityDpi} DPI", TestStatus.PASSED),
        TestDetail("Плотность (px/dp)", String.format("%.2f", density), TestStatus.PASSED),
        TestDetail("Частота обновления", String.format("%.1f Гц", refreshRate), TestStatus.PASSED),
        TestDetail("Размер экрана", String.format("%.2f дюймов", screenDiagonalInches), TestStatus.PASSED),
        TestDetail("HDR поддержка", if (isHdrSupported) "Да" else "Нет", if (isHdrSupported) TestStatus.PASSED else TestStatus.SKIPPED),
        TestDetail("HDR типы", hdrType, if (isHdrSupported) TestStatus.PASSED else TestStatus.SKIPPED),
        TestDetail("Цветовой режим", colorMode, TestStatus.PASSED),
    )

    // Report result immediately (display info is always available)
    LaunchedEffect(Unit) {
        viewModel.updateResult(
            TestCategory.DISPLAY_INFO,
            TestResult(TestCategory.DISPLAY_INFO, TestStatus.PASSED, details)
        )
    }

    TestScreenScaffold(
        title = "Информация о дисплее",
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Resolution highlight card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${resolutionWidth} × ${resolutionHeight}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "пикселей",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Характеристики экрана",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            details.forEach { detail ->
                DetailRow(
                    label = detail.label,
                    value = detail.value,
                    status = detail.status
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Additional info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Расчётные данные",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(
                        label = "Ширина (дюймы)",
                        value = String.format("%.2f\"", screenWidthInches)
                    )
                    DetailRow(
                        label = "Высота (дюймы)",
                        value = String.format("%.2f\"", screenHeightInches)
                    )
                    DetailRow(
                        label = "Общее пикселей",
                        value = String.format("%,d", resolutionWidth.toLong() * resolutionHeight.toLong())
                    )
                }
            }
        }
    }
}