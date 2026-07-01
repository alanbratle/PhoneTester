package com.phonetester.app.ui.screens

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestResult
import com.phonetester.app.model.TestStatus
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current

    var hasPermission by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var isPreviewRunning by remember { mutableStateOf(false) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            permissionDenied = true
            viewModel.updateResult(
                TestCategory.CAMERA,
                TestResult(
                    category = TestCategory.CAMERA,
                    status = TestStatus.SKIPPED,
                    details = emptyList()
                )
            )
            Toast.makeText(ctx, "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val currentPermission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
        if (currentPermission == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            hasPermission = true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Clean up camera on dispose
    DisposableEffect(Unit) {
        onDispose {
            cameraProviderInstance?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    // Bind / rebind camera when permission or camera selection changes
    LaunchedEffect(hasPermission, useFrontCamera) {
        if (!hasPermission) return@LaunchedEffect

        val providerFuture = ProcessCameraProvider.getInstance(ctx)
        providerFuture.get().let { provider ->
            cameraProviderInstance = provider

            // Unbind any existing use cases
            provider.unbindAll()

            val cameraSelector = if (useFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val previewUseCase = Preview.Builder().build()

            try {
                provider.bindToLifecycle(
                    (ctx as? androidx.activity.ComponentActivity)
                        ?: return@LaunchedEffect,
                    cameraSelector,
                    previewUseCase
                )
                isPreviewRunning = true
            } catch (e: Exception) {
                isPreviewRunning = false
                Toast.makeText(ctx, "Ошибка камеры: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    TestScreenScaffold(
        title = "Тест камеры",
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
                    text = "⚠️ Разрешение на камеру не предоставлено.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Тест пропущен. Разрешите доступ к камере в настройках.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = {
                    permissionDenied = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text("Запросить разрешение")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Camera preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasPermission) {
                        AndroidView(
                            factory = { factoryContext ->
                                PreviewView(factoryContext).also { previewView ->
                                    // Bind the surface provider once the preview use case is ready
                                    try {
                                        val provider = ProcessCameraProvider.getInstance(factoryContext).get()
                                        val cameraSelector = if (useFrontCamera) {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        } else {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        }
                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        provider.unbindAll()
                                        provider.bindToLifecycle(
                                            factoryContext as androidx.lifecycle.LifecycleOwner,
                                            cameraSelector,
                                            preview
                                        )
                                    } catch (_: Exception) {
                                        // Preview binding handled in LaunchedEffect fallback
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = "Запрос разрешения...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Camera info label
                Text(
                    text = if (useFrontCamera) "Передняя камера" else "Задняя камера",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Switch camera button
                    OutlinedButton(
                        onClick = {
                            useFrontCamera = !useFrontCamera
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Переключить камеру")
                    }

                    // Mark as passed button
                    Button(
                        onClick = {
                            val details = listOf(
                                com.phonetester.app.model.TestDetail(
                                    label = "Превью камеры",
                                    value = if (useFrontCamera) "Передняя — OK" else "Задняя — OK",
                                    status = TestStatus.PASSED
                                )
                            )
                            viewModel.updateResult(
                                TestCategory.CAMERA,
                                TestResult(
                                    category = TestCategory.CAMERA,
                                    status = TestStatus.PASSED,
                                    details = details
                                )
                            )
                            Toast.makeText(ctx, "Камера: Пройден", Toast.LENGTH_SHORT).show()
                        },
                        enabled = isPreviewRunning,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Пройден")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mark as failed button
                OutlinedButton(
                    onClick = {
                        viewModel.updateResult(
                            TestCategory.CAMERA,
                            TestResult(
                                category = TestCategory.CAMERA,
                                status = TestStatus.FAILED,
                                details = listOf(
                                    com.phonetester.app.model.TestDetail(
                                        label = "Превью камеры",
                                        value = "Не удалось получить изображение",
                                        status = TestStatus.FAILED
                                    )
                                )
                            )
                        )
                        Toast.makeText(ctx, "Камера: Не пройден", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Не пройден")
                }
            }
        }
    }
}