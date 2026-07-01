package com.phonetester.app.ui.screens

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
    var testDone by remember { mutableStateOf(false) }
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
            val details = mutableListOf<TestDetail>()
            try {
                val testFile = File(context.filesDir, TEST_FILE_NAME)
                val writeTimeMs = withContext(Dispatchers.IO) {
                    val data = ByteArray(TEST_FILE_SIZE.toInt())
                    for (i in data.indices) { data[i] = (i % 256).toByte() }
                    val start = System.nanoTime()
                    FileOutputStream(testFile).use { it.write(data); it.flush() }
                    (System.nanoTime() - start) / 1_000_000.0
                }
                val wMbps = (TEST_FILE_SIZE / 1048576.0) / (writeTimeMs / 1000.0)
                writeSpeed = wMbps.toFloat()
                details.add(TestDetail("Write Speed", String.format("%.2f MB/s", wMbps)))
                progress = 0.5f
                progressLabel = "Reading test file..."
                val readTimeMs = withContext(Dispatchers.IO) {
                    val buf = ByteArray(8192)
                    val start = System.nanoTime()
                    FileInputStream(testFile).use { fis ->
                        while (fis.read(buf) != -1) { }
                    }
                    (System.nanoTime() - start) / 1_000_000.0
                }
                val rMbps = (TEST_FILE_SIZE / 1048576.0) / (readTimeMs / 1000.0)
                readSpeed = rMbps.toFloat()
                details.add(TestDetail("Read Speed", String.format("%.2f MB/s", rMbps)))
                progress = 0.9f
                progressLabel = "Cleaning up..."
                withContext(Dispatchers.IO) { testFile.delete() }
                progress = 1f
                progressLabel = "Complete"
                details.add(TestDetail("Total Storage", totalSpace))
                details.add(TestDetail("Used Storage", usedSpace))
                details.add(TestDetail("Free Storage", freeSpace))
                val passed = wMbps > 0 && rMbps > 0
                viewModel.updateResult(TestCategory.STORAGE, TestResult(TestCategory.STORAGE, if (passed) TestStatus.PASSED else TestStatus.FAILED, details))
            } catch (e: Exception) {
                progressLabel = "Error: ${e.message}"
                viewModel.updateResult(TestCategory.STORAGE, TestResult(TestCategory.STORAGE, TestStatus.FAILED, details))
                withContext(Dispatchers.IO) { File(context.filesDir, TEST_FILE_NAME).delete() }
            } finally { isRunning = false; testDone = true }
        }
    }

    TestScreenScaffold(title = "Storage Test", onBack = onBack) { pv ->
        Column(modifier = Modifier.fillMaxSize().padding(pv).padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Storage Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DetailRow("Total Space", totalSpace)
            DetailRow("Used Space", usedSpace)
            DetailRow("Free Space", freeSpace)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Speed Test (10 MB)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (isRunning) {
                Text(progressLabel, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
            }
            writeSpeed?.let { DetailRow("Write Speed", String.format("%.2f MB/s", it), if (it > 0) TestStatus.PASSED else TestStatus.FAILED) }
            readSpeed?.let { DetailRow("Read Speed", String.format("%.2f MB/s", it), if (it > 0) TestStatus.PASSED else TestStatus.FAILED) }
        }
    }
}