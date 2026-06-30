package com.phonetester.app.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestStatus
import com.phonetester.app.ui.components.ProgressRing
import com.phonetester.app.ui.theme.StatusFailed
import com.phonetester.app.ui.theme.StatusPassed
import com.phonetester.app.ui.theme.StatusSkipped
import com.phonetester.app.viewmodel.DashboardViewModel
import com.phonetester.app.viewmodel.TestResultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultScreen(
    context: Context,
    dashboardViewModel: DashboardViewModel = viewModel(),
    resultViewModel: TestResultViewModel = viewModel(),
    onBack: () -> Unit
) {
    val results by dashboardViewModel.results.collectAsStateWithLifecycle()
    val deviceInfo by resultViewModel.deviceInfo

    var showExportDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    LaunchedEffect(Unit) {
        resultViewModel.loadDeviceInfo(context)
    }

    if (showExportDialog) {
        val report = buildString {
            appendLine("=== Phone Tester Pro — Отчёт ===")
            appendLine()
            deviceInfo?.let { info ->
                appendLine("Устройство: ${info.manufacturer} ${info.model}")
                appendLine("Android: ${info.androidVersion} (SDK ${info.sdkVersion})")
                appendLine("RAM: ${info.totalRam / (1024 * 1024)} МБ")
                appendLine("Ядра CPU: ${info.cpuCores}")
                appendLine("Архитектура: ${info.cpuAbi}")
                appendLine()
            }
            appendLine("=== Результаты тестов ===")
            TestCategory.all.forEach { cat ->
                val r = results[cat]
                val statusStr = when (r?.status) {
                    TestStatus.PASSED -> "OK"
                    TestStatus.FAILED -> "FAIL"
                    TestStatus.SKIPPED -> "SKIP"
                    else -> "PENDING"
                }
                appendLine("  ${cat.displayName}: $statusStr")
                r?.details?.forEach { d ->
                    appendLine("    - ${d.label}: ${d.value}")
                }
            }
            val passed = results.values.count { it.status == TestStatus.PASSED }
            val total = TestCategory.all.size
            appendLine()
            appendLine("Итого: $passed / $total тестов пройдено")
            appendLine("Дата: ${java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())}")
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Результаты скопированы") },
            text = {
                Text(
                    text = "Отчёт скопирован в буфер обмена.\n\n${report.take(300)}...",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                FilledTonalButton(onClick = { showExportDialog = false }) {
                    Text("OK")
                }
            }
        )
        LaunchedEffect(Unit) {
            clipboardManager.setText(AnnotatedString(report))
        }
    }

    val passedCount = results.values.count { it.status == TestStatus.PASSED }
    val failedCount = results.values.count { it.status == TestStatus.FAILED }
    val totalTests = TestCategory.all.size
    val progress = if (totalTests > 0) passedCount.toFloat() / totalTests else 0f

    val allPassed = passedCount == totalTests
    val anyFailed = failedCount > 0

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Результаты тестирования") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Filled.ContentCopy, "Копировать")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Summary Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (allPassed) StatusPassed.copy(alpha = 0.1f)
                        else if (anyFailed) StatusFailed.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProgressRing(
                            progress = progress,
                            size = 140.dp,
                            foregroundColor = if (allPassed) StatusPassed
                            else if (anyFailed) StatusFailed
                            else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (allPassed) "Все тесты пройдены!"
                            else "Пройдено: $passedCount из $totalTests",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (allPassed) StatusPassed else MaterialTheme.colorScheme.onSurface
                        )
                        if (anyFailed) {
                            Text(
                                text = "Некоторые тесты не пройдены",
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusFailed,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Device Info Card
            deviceInfo?.let { info ->
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Устройство",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            InfoRow("Модель", "${info.manufacturer} ${info.model}")
                            InfoRow("Android", info.androidVersion)
                            InfoRow("SDK", info.sdkVersion.toString())
                            InfoRow("RAM", "${info.totalRam / (1024 * 1024)} МБ")
                            InfoRow("CPU ядра", info.cpuCores.toString())
                            InfoRow("Архитектура", info.cpuAbi)
                        }
                    }
                }
            }

            // Per-test results
            items(TestCategory.all) { category ->
                val result = results[category]
                val status = result?.status ?: TestStatus.PENDING
                val statusColor = when (status) {
                    TestStatus.PASSED -> StatusPassed
                    TestStatus.FAILED -> StatusFailed
                    else -> StatusSkipped
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (status == TestStatus.PASSED) Icons.Filled.CheckCircle
                            else Icons.Filled.Close,
                            contentDescription = null,
                            tint = statusColor
                        )
                        Spacer(modifier = Modifier.padding(start = 12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                category.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            result?.details?.forEach { detail ->
                                Text(
                                    "${detail.label}: ${detail.value}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.6f)
        )
    }
}