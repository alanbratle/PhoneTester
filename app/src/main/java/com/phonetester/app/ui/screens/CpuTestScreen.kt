package com.phonetester.app.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phonetester.app.model.*
import com.phonetester.app.ui.components.DetailRow
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun CpuTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var testStatus by remember { mutableStateOf(TestStatus.PENDING) }
    var progress by remember { mutableFloatStateOf(0f) }
    var coreCount by remember { mutableIntStateOf(0) }
    var cpuAbi by remember { mutableStateOf("N/A") }
    var maxFrequencies by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }
    var benchmarkScore by remember { mutableStateOf<String?>(null) }
    var benchmarkTimeMs by remember { mutableStateOf<String?>(null) }
    var primeCount by remember { mutableIntStateOf(0) }
    var details by remember { mutableStateOf<List<TestDetail>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        testStatus = TestStatus.TESTING
        viewModel.updateResult(TestCategory.CPU, TestResult(TestCategory.CPU, TestStatus.TESTING))

        // Gather CPU info
        val cores = Runtime.getRuntime().availableProcessors()
        coreCount = cores
        cpuAbi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Build.SUPPORTED_ABIS.joinToString(", ")
        } else {
            @Suppress("DEPRECATION")
            Build.CPU_ABI
        }

        val freqs = mutableListOf<Pair<Int, String>>()
        for (i in 0 until cores) {
            val freqFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            if (freqFile.exists() && freqFile.canRead()) {
                try {
                    val freqKhz = freqFile.readText().trim().toLong()
                    val freqMhz = freqKhz / 1000
                    freqs.add(i to "$freqMhz MHz")
                } catch (_: Exception) {
                    freqs.add(i to "N/A")
                }
            } else {
                freqs.add(i to "N/A")
            }
        }
        maxFrequencies = freqs
        progress = 0.2f

        // Run benchmark on Default dispatcher (CPU-intensive)
        val primeLimit = 500_000
        val startTime = System.nanoTime()

        val count = coroutineScope.launch(Dispatchers.Default) {
            var found = 0
            for (n in 2..primeLimit) {
                if (isPrime(n)) {
                    found++
                }
                if (n % 50_000 == 0) {
                    progress = 0.2f + 0.8f * (n.toFloat() / primeLimit)
                }
            }
            found
        }.join()

        val elapsedMs = (System.nanoTime() - startTime) / 1_000_000
        primeCount = count
        benchmarkTimeMs = "${elapsedMs} мс"

        val opsPerSecond = if (elapsedMs > 0) {
            val ops = (primeLimit.toLong() * 1000) / elapsedMs
            String.format("%,d", ops)
        } else {
            "∞"
        }
        benchmarkScore = "$opsPerSecond оп/с"

        details = buildList {
            add(TestDetail("Ядра CPU", "$coreCount"))
            add(TestDetail("Архитектура (ABI)", cpuAbi))
            maxFrequencies.forEach { (core, freq) ->
                add(TestDetail("Ядро $core макс. частота", freq))
            }
            add(TestDetail("Простые числа до $primeLimit", "$primeCount"))
            add(TestDetail("Время бенчмарка", benchmarkTimeMs ?: "N/A"))
            add(TestDetail("Производительность", benchmarkScore ?: "N/A"))
        }

        testStatus = TestStatus.PASSED
        viewModel.updateResult(
            TestCategory.CPU,
            TestResult(TestCategory.CPU, TestStatus.PASSED, details)
        )
    }

    TestScreenScaffold(
        title = "Тест процессора",
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (testStatus == TestStatus.TESTING) {
                Text(
                    text = "Выполнение бенчмарка...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (details.isNotEmpty()) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Информация о CPU",
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
            }

            if (testStatus == TestStatus.PASSED && benchmarkScore != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Результат бенчмарка",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = benchmarkScore!!,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "операций в секунду",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Время: ${benchmarkTimeMs ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (testStatus == TestStatus.FAILED) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "Ошибка при выполнении теста",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun isPrime(n: Int): Boolean {
    if (n < 2) return false
    if (n == 2 || n == 3) return true
    if (n % 2 == 0 || n % 3 == 0) return false
    var i = 5
    while (i * i <= n) {
        if (n % i == 0 || n % (i + 2) == 0) return false
        i += 6
    }
    return true
}