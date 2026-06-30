package com.phonetester.app.ui.screens

import android.Manifest
import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestDetail
import com.phonetester.app.model.TestResult
import com.phonetester.app.model.TestStatus
import com.phonetester.app.ui.components.DetailRow
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Composable
fun AudioTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var hasPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val tabTitles = listOf("Динамик", "Микрофон")

    // Speaker state
    var tonePlaying by remember { mutableStateOf(false) }

    // Microphone state
    var isRecording by remember { mutableStateOf(false) }
    var isRecorded by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0L) } // in seconds
    var recordingStartTime by remember { mutableStateOf(0L) }
    var recorderFile by remember { mutableStateOf<File?>(null) }

    val mediaRecorder = remember { mutableStateOf<MediaRecorder?>(null) }
    val mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }
    val scheduler = remember { Executors.newSingleThreadScheduledExecutor() }
    var timerFuture by remember { mutableStateOf<ScheduledFuture<*>?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            permissionDenied = true
            viewModel.updateResult(
                TestCategory.AUDIO,
                TestResult(
                    category = TestCategory.AUDIO,
                    status = TestStatus.SKIPPED,
                    details = emptyList()
                )
            )
            Toast.makeText(ctx, "Разрешение на микрофон отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val currentPermission =
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO)
        if (currentPermission == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Clean up on dispose
    DisposableEffect(Unit) {
        onDispose {
            timerFuture?.cancel(false)
            scheduler.shutdown()
            try {
                mediaRecorder.value?.stop()
                mediaRecorder.value?.release()
            } catch (_: Exception) {}
            try {
                mediaPlayer.value?.stop()
                mediaPlayer.value?.release()
            } catch (_: Exception) {}
        }
    }

    // Helper: update timer while recording
    fun startTimer() {
        timerFuture = scheduler.scheduleAtFixedRate({
            if (isRecording) {
                val elapsed = (System.currentTimeMillis() - recordingStartTime) / 1000
                recordingDuration = elapsed
            }
        }, 0, 1, TimeUnit.SECONDS)
    }

    fun stopTimer() {
        timerFuture?.cancel(false)
        timerFuture = null
    }

    // Play test tone using ToneGenerator
    fun playTestTone() {
        if (tonePlaying) return
        tonePlaying = true
        Thread {
            try {
                val toneGenerator = ToneGenerator(
                    android.media.AudioManager.STREAM_MUSIC,
                    100
                )
                toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, 500)
                Thread.sleep(600)
                toneGenerator.release()
            } catch (_: Exception) {
                // ToneGenerator may fail on some devices
            }
            tonePlaying = false
        }.start()
    }

    // Start recording
    fun startRecording() {
        try {
            val file = File(ctx.cacheDir, "audio_test_${System.currentTimeMillis()}.3gp")
            recorderFile = file

            val recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder.value = recorder
            isRecording = true
            isRecorded = false
            recordingDuration = 0L
            recordingStartTime = System.currentTimeMillis()
            startTimer()
        } catch (e: Exception) {
            Toast.makeText(ctx, "Ошибка записи: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Stop recording
    fun stopRecording() {
        try {
            mediaRecorder.value?.apply {
                stop()
                release()
            }
            mediaRecorder.value = null
        } catch (_: Exception) {}
        isRecording = false
        isRecorded = true
        stopTimer()
    }

    // Play back recording
    fun playRecording() {
        val file = recorderFile
        if (file == null || !file.exists()) {
            Toast.makeText(ctx, "Нет записи для воспроизведения", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    isPlaying = false
                    release()
                    mediaPlayer.value = null
                }
                setOnErrorListener { _, _, _ ->
                    isPlaying = false
                    release()
                    mediaPlayer.value = null
                    true
                }
                prepare()
                start()
            }
            mediaPlayer.value = player
            isPlaying = true
        } catch (e: Exception) {
            Toast.makeText(ctx, "Ошибка воспроизведения: ${e.message}", Toast.LENGTH_SHORT).show()
            isPlaying = false
        }
    }

    TestScreenScaffold(
        title = "Тест аудио",
        onBack = onBack
    ) { paddingValues: PaddingValues ->
        if (permissionDenied) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚠️ Разрешение на микрофон не предоставлено.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Тест пропущен. Разрешите доступ к микрофону в настройках.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    permissionDenied = false
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }) {
                    Text("Запросить разрешение")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    tabTitles.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    0 -> {
                        // Speaker test tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = "Тест динамика",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Нажмите кнопку, чтобы воспроизвести тестовый сигнал через динамик устройства.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = { playTestTone() },
                                enabled = !tonePlaying,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (tonePlaying) "Воспроизведение..." else "Воспроизвести звук")
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (tonePlaying) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🔊",
                                        fontSize = 48.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Mark passed/failed for speaker
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateResult(
                                            TestCategory.AUDIO,
                                            TestResult(
                                                category = TestCategory.AUDIO,
                                                status = TestStatus.FAILED,
                                                details = listOf(
                                                    TestDetail(
                                                        label = "Динамик",
                                                        value = "Не пройден",
                                                        status = TestStatus.FAILED
                                                    )
                                                )
                                            )
                                        )
                                        Toast.makeText(ctx, "Динамик: Не пройден", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Не пройден")
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateResult(
                                            TestCategory.AUDIO,
                                            TestResult(
                                                category = TestCategory.AUDIO,
                                                status = TestStatus.PASSED,
                                                details = listOf(
                                                    TestDetail(
                                                        label = "Динамик",
                                                        value = "Звук воспроизведён",
                                                        status = TestStatus.PASSED
                                                    )
                                                )
                                            )
                                        )
                                        Toast.makeText(ctx, "Динамик: Пройден", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Пройден")
                                }
                            }
                        }
                    }

                    1 -> {
                        // Microphone test tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = "Тест микрофона",
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Запишите короткий фрагмент, затем воспроизведите его для проверки микрофона.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Recording info
                            if (isRecording) {
                                DetailRow(
                                    label = "Длительность записи",
                                    value = formatDuration(recordingDuration)
                                )
                            } else if (isRecorded && recordingDuration > 0) {
                                DetailRow(
                                    label = "Записано",
                                    value = formatDuration(recordingDuration)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Recording indicator
                            if (isRecording) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🔴 Запись...",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            // Control buttons
                            if (!isRecording && !isRecorded) {
                                // Show start recording button
                                Button(
                                    onClick = { startRecording() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Начать запись")
                                }
                            } else if (isRecording) {
                                // Show stop button
                                Button(
                                    onClick = { stopRecording() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Остановить запись")
                                }
                            } else if (isRecorded) {
                                // Show playback controls
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { playRecording() },
                                        enabled = !isPlaying,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isPlaying) "Воспроизведение..." else "Воспроизвести запись")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            // Allow re-recording
                                            isRecorded = false
                                            recordingDuration = 0L
                                            recorderFile = null
                                        },
                                        enabled = !isPlaying,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Записать заново")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Mark passed/failed for microphone
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateResult(
                                            TestCategory.AUDIO,
                                            TestResult(
                                                category = TestCategory.AUDIO,
                                                status = TestStatus.FAILED,
                                                details = listOf(
                                                    TestDetail(
                                                        label = "Микрофон",
                                                        value = "Не пройден",
                                                        status = TestStatus.FAILED
                                                    )
                                                )
                                            )
                                        )
                                        Toast.makeText(ctx, "Микрофон: Не пройден", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Не пройден")
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateResult(
                                            TestCategory.AUDIO,
                                            TestResult(
                                                category = TestCategory.AUDIO,
                                                status = TestStatus.PASSED,
                                                details = listOf(
                                                    TestDetail(
                                                        label = "Микрофон",
                                                        value = "Запись выполнена (${formatDuration(recordingDuration)})",
                                                        status = TestStatus.PASSED
                                                    ),
                                                    TestDetail(
                                                        label = "Воспроизведение",
                                                        value = "Успешно",
                                                        status = TestStatus.PASSED
                                                    )
                                                )
                                            )
                                        )
                                        Toast.makeText(ctx, "Микрофон: Пройден", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Пройден")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Formats duration in seconds to mm:ss string.
 */
private fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}