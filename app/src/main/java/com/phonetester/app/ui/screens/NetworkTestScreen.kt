package com.phonetester.app.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL

@Composable
fun NetworkTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var testStatus by remember { mutableStateOf(TestStatus.PENDING) }
    var isRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var connectionType by remember { mutableStateOf("Определение...") }
    var downloadSpeed by remember { mutableStateOf<String?>(null) }
    var uploadEstimate by remember { mutableStateOf<String?>(null) }
    var latency by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var details by remember { mutableStateOf<List<TestDetail>>(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isRunning = true
        testStatus = TestStatus.TESTING
        viewModel.updateResult(
            TestCategory.NETWORK,
            TestResult(TestCategory.NETWORK, TestStatus.TESTING)
        )

        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)

            if (caps == null) {
                connectionType = "Нет подключения"
                errorMessage = "Нет активного сетевого подключения"
                testStatus = TestStatus.SKIPPED
                details = listOf(
                    TestDetail("Тип подключения", "Нет", TestStatus.FAILED),
                    TestDetail("Статус", "Пропущено — нет сети", TestStatus.SKIPPED)
                )
                viewModel.updateResult(
                    TestCategory.NETWORK,
                    TestResult(TestCategory.NETWORK, TestStatus.SKIPPED, details)
                )
                isRunning = false
                return@LaunchedEffect
            }

            connectionType = when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Мобильная сеть"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Другое"
            }
            progress = 0.1f

            // Ping test
            val pingResult = withContext(Dispatchers.IO) {
                runPingTest("google.com", 5)
            }
            latency = pingResult
            progress = 0.3f

            // Download speed test
            val speedResult = withContext(Dispatchers.IO) {
                runDownloadSpeedTest()
            }
            downloadSpeed = speedResult.first
            progress = 0.9f

            // Upload estimate (rough: usually 30-70% of download on mobile, similar on WiFi)
            val downloadMbps = speedResult.second
            val uploadFactor = if (connectionType == "Wi-Fi") 0.85 else 0.5
            uploadEstimate = String.format("%.2f Мбит/с (оценка)", downloadMbps * uploadFactor)

            progress = 1f

            details = buildList {
                add(TestDetail("Тип подключения", connectionType, TestStatus.PASSED))
                if (latency != null) {
                    add(
                        TestDetail(
                            "Задержка (ping)",
                            latency!!,
                            if (latency!!.removeSuffix(" мс").toIntOrNull()?.let { it < 100 } == true)
                                TestStatus.PASSED else TestStatus.FAILED
                        )
                    )
                }
                if (downloadSpeed != null) {
                    add(
                        TestDetail(
                            "Скорость загрузки",
                            downloadSpeed!!,
                            TestStatus.PASSED
                        )
                    )
                }
                if (uploadEstimate != null) {
                    add(
                        TestDetail(
                            "Оценка скорости отдачи",
                            uploadEstimate!!,
                            TestStatus.PASSED
                        )
                    )
                }
            }

            testStatus = TestStatus.PASSED
            viewModel.updateResult(
                TestCategory.NETWORK,
                TestResult(TestCategory.NETWORK, TestStatus.PASSED, details)
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "Неизвестная ошибка"
            testStatus = TestStatus.SKIPPED
            details = listOf(
                TestDetail("Тип подключения", connectionType, TestStatus.PASSED),
                TestDetail("Ошибка", errorMessage ?: "Неизвестная ошибка", TestStatus.FAILED)
            )
            viewModel.updateResult(
                TestCategory.NETWORK,
                TestResult(TestCategory.NETWORK, TestStatus.SKIPPED, details)
            )
        }

        isRunning = false
    }

    TestScreenScaffold(
        title = "Тест сети",
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRunning) {
                Text(
                    text = "Тестирование сети...",
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
                    text = "Результаты",
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

            if (testStatus == TestStatus.SKIPPED && errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (testStatus == TestStatus.PASSED) {
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
                            text = "Скорость загрузки",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = downloadSpeed ?: "N/A",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Задержка: ${latency ?: "N/A"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

private fun runPingTest(host: String, count: Int): String {
    val rtts = mutableListOf<Long>()
    for (i in 0 until count) {
        try {
            val address = InetAddress.getByName(host)
            val start = System.nanoTime()
            val reachable = address.isReachable(3000)
            val elapsed = (System.nanoTime() - start) / 1_000_000
            if (reachable) {
                rtts.add(elapsed)
            }
        } catch (_: Exception) {
            // Skip failed pings
        }
    }
    return if (rtts.isEmpty()) {
        "Таймаут"
    } else {
        val avg = rtts.average()
        String.format("%.0f мс", avg)
    }
}

private fun runDownloadSpeedTest(): Pair<String, Double> {
    val testUrl = "http://speedtest.tele2.net/1MB.zip"
    var connection: HttpURLConnection? = null
    try {
        val url = URL(testUrl)
        connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        connection.instanceFollowRedirects = true
        connection.connect()

        val contentLength = connection.contentLengthLong
        val inputStream: java.io.InputStream = connection.inputStream
        val buffer = ByteArray(8192)
        var totalBytesRead = 0L

        val startTime = System.nanoTime()

        while (true) {
            val bytesRead = inputStream.read(buffer)
            if (bytesRead == -1) break
            totalBytesRead += bytesRead
        }

        val elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0
        inputStream.close()

        if (elapsedSeconds <= 0 || totalBytesRead <= 0) {
            return "Не удалось измерить" to 0.0
        }

        val bitsPerSecond = (totalBytesRead * 8) / elapsedSeconds
        val mbps = bitsPerSecond / 1_000_000.0
        return String.format("%.2f Мбит/с", mbps) to mbps
    } catch (e: Exception) {
        return "Ошибка: ${e.localizedMessage}" to 0.0
    } finally {
        connection?.disconnect()
    }
}