package com.phonetester.app.ui.screens

import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
fun TestResultScreen(context: Context, dashboardViewModel: DashboardViewModel = viewModel(), resultViewModel: TestResultViewModel = viewModel(), onBack: () -> Unit) {
    val results by dashboardViewModel.results.collectAsState()
    val deviceInfo by resultViewModel.deviceInfo.collectAsState()
    var showExportDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    LaunchedEffect(Unit) { resultViewModel.loadDeviceInfo(context) }
    val report = remember(results, deviceInfo) {
        buildString {
            appendLine("=== Phone Tester Pro ===")
            deviceInfo?.let { appendLine("Device: ${it.manufacturer} ${it.model}"); appendLine("Android: ${it.androidVersion}"); appendLine() }
            TestCategory.all.forEach { cat ->
                val r = results[cat]; appendLine("${cat.displayName}: ${when(r?.status){TestStatus.PASSED->"OK";TestStatus.FAILED->"FAIL";else->"PENDING"}}")
            }
            appendLine("Passed: ${results.values.count { it.status == TestStatus.PASSED }} / ${TestCategory.all.size}")
        }
    }
    if (showExportDialog) {
        LaunchedEffect(Unit) { clipboardManager.setText(AnnotatedString(report)) }
        AlertDialog(onDismissRequest = { showExportDialog = false }, title = { Text("Copied") },
            text = { Text(report.take(300) + "...", style = MaterialTheme.typography.bodySmall) },
            confirmButton = { FilledTonalButton(onClick = { showExportDialog = false }) { Text("OK") } })
    }
    val pc = results.values.count { it.status == TestStatus.PASSED }
    val fc = results.values.count { it.status == TestStatus.FAILED }
    val tt = TestCategory.all.size
    val pr = if (tt > 0) pc.toFloat() / tt else 0f
    Scaffold(modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), topBar = {
        TopAppBar(title = { Text("Results") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            actions = { IconButton(onClick = { showExportDialog = true }) { Icon(Icons.Filled.ContentCopy, "Copy") } })
    }) { pad ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { ElevatedCard(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.elevatedCardColors(containerColor = if (pc==tt) StatusPassed.copy(alpha=0.1f) else if (fc>0) StatusFailed.copy(alpha=0.1f) else MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProgressRing(progress = pr, size = 140.dp, foregroundColor = if (pc==tt) StatusPassed else if (fc>0) StatusFailed else MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(if (pc==tt) "All passed!" else "Passed: $pc / $tt", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (pc==tt) StatusPassed else MaterialTheme.colorScheme.onSurface)
                    if (fc > 0) { Text("Some tests failed", style = MaterialTheme.typography.bodyMedium, color = StatusFailed, modifier = Modifier.padding(top = 4.dp)) }
                } } }
            deviceInfo?.let { info -> item { Card(modifier = Modifier.fillMaxWidth()) { Column(modifier = Modifier.padding(16.dp)) {
                Text("Device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp))
                InfoRow("Model", "${info.manufacturer} ${info.model}"); InfoRow("Android", info.androidVersion); InfoRow("SDK", info.sdkVersion.toString())
                InfoRow("RAM", "${info.totalRam / 1048576} MB"); InfoRow("CPU", "${info.cpuCores} cores"); InfoRow("ABI", info.cpuAbi)
            } } } }
            items(TestCategory.all) { cat -> val st = results[cat]?.status ?: TestStatus.PENDING; val sc = when(st){TestStatus.PASSED->StatusPassed;TestStatus.FAILED->StatusFailed;else->StatusSkipped}
                Card(modifier = Modifier.fillMaxWidth()) { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (st==TestStatus.PASSED) Icons.Filled.CheckCircle else Icons.Filled.Close, null, tint = sc)
                    Spacer(Modifier.padding(start = 12.dp))
                    Column(modifier = Modifier.weight(1f)) { Text(cat.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                        results[cat]?.details?.forEach { d -> Text("${d.label}: ${d.value}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } } } }
            item { Spacer(Modifier.height(16.dp)) }
        } } }

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f))
    }
}