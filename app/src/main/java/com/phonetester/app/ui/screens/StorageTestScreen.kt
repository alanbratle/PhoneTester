package com.phonetester.app.ui.screens

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestDetail
import com.phonetester.app.model.TestResult
import com.phonetester.app.model.TestStatus
import com.phonetester.app.ui.components.DetailRow
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

private const val TEST_FILE_NAME = "storage_speed_test.tmp"
private const val TEST_FILE_SIZE = 10 * 1024 * 1024L

@Composable
fun StorageTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var totalSpace by remember { mutableStateOf("---") }
    var usedSpace by remember { mutableStateOf("---") }
    var freeSpace by remember { mutableStateOf("---") }
    var writeSpeed by remember { mutableStateOf<Float?>(null) }
    var readSpeed by remember { mutableStateOf<Float?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var progressLabel by remember { mutableStateOf("Preparing...") }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val statFs = StatFs(Environment.getDataDirectory().path)
        totalSpace = Formatter.formatFileSize(context, statFs.totalBytes)
        usedSpace = Formatter.formatFileSize(context, statFs.totalBytes - statFs.freeBytes)
        freeSpace = Formatter.formatFileSize(context, statFs.freeBytes)
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            isRunning = true
            progressLabel = "Writing test file..."
            progress = 0f
            val details = mutableListOf<TestDetail>()

            try {
                val testFile = File(context.filesDir, TEST_FILE_NAME)

                val writeTimeMs = withContext(Dispatchers.IO) {
                    val data = ByteArray(TEST_FILE_SIZE.toInt())
                    for (i in data.indices) { data[i] = (i % 256).toByte() }
                    val startTime = System.nanoTime()
                    FileOutputStream(testFile).use { it.write(data); it.flush() }
                    (System.nanoTime() - startTime) / 1_000_000.0
                }

                val writeMbps = (TEST_FILE_SIZE / 1024.0 / 1024.0) / (writeTimeMs / 1000.0)
                writeSpeed = writeMbps
                details.add(TestDetail("Write Speed", String.format("%.2f MB/s", writeMbps), TestStatus.PASSED))

                progress = 0.5f
                progressLabel = "Reading test file..."

                val readTimeMs = withContext(Dispatchers.IO) {
                    val buffer = ByteArray(8192)
                    val startTime = System.nanoTime()
                    FileInputStream(testFile).use { fis ->
                        var bytesRead: Int
                        while (fis.read(buffer).also { bytesRead = it } != -1) {}
                    }
                    (System.nanoTime() - startTime) / 1_000_000.0
                }

                val readMbps = (TEST_FILE_SIZE / 1024.0 / 1024.0) / (readTimeMs / 1000.0)
                readSpeed = readMbps
                details.add(TestDetail("Read Speed", String.format("%.2f MB/s", readMbps), TestStatus.PASSED))

                progress = 0.9f
                progressLabel = "Cleaning up..."

                withContext(Dispatchers.IO) { testFile.delete() }

                progress = 1f
                details.add(TestDetail("Total Storage", totalSpace, TestStatus.PASSED))
                details.add(TestDetail("Used Storage", usedSpace, TestStatus.PASSED))
                details.add(TestDetail("Free Storage", freeSpace, TestStatus.PASSED))

                viewModel.updateResult(
                    TestCategory.STORAGE,
                    TestResult(TestCategory.STORAGE, TestStatus.PASSED, details)
                )
            } catch (e: Exception) {
                progressLabel = "Error: ${e.message}"
                details.add(TestDetail("Error", e.message ?: "Unknown", TestStatus.FAILED))
                viewModel.updateResult(
                    TestCategory.STORAGE,
                    TestResult(TestCategory.STORAGE, TestStatus.FAILED, details)
                )
                withContext(Dispatchers.IO) {
                    File(context.filesDir, TEST_FILE_NAME).delete()
                }
            } finally {
                isRunning = false
            }
        }
    }

    TestScreenScaffold(
        title = "Storage Test",
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Storage Information",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            DetailRow(label = "Total Space", value = totalSpace)
            DetailRow(label = "Used Space", value = usedSpace)
            DetailRow(label = "Free Space", value = freeSpace)

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Text(
                text = "Speed Test (10 MB)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (isRunning) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (writeSpeed != null) {
                Spacer(modifier = Modifier.height(8.dp))
                DetailRow(
                    label = "Write Speed",
                    value = String.format("%.2f MB/s", writeSpeed!!)
                )
            }

            if (readSpeed != null) {
                DetailRow(
                    label = "Read Speed",
                    value = String.format("%.2f MB/s", readSpeed!!)
                )
            }
        }
    }
}