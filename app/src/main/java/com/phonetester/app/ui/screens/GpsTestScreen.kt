package com.phonetester.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.phonetester.app.model.*
import com.phonetester.app.ui.components.DetailRow
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@Composable
fun GpsTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var testStatus by remember { mutableStateOf(TestStatus.PENDING) }
    var searchMessage by remember { mutableStateOf("Подготовка GPS...") }
    var latitude by remember { mutableStateOf<String?>(null) }
    var longitude by remember { mutableStateOf<String?>(null) }
    var accuracy by remember { mutableStateOf<String?>(null) }
    var altitude by remember { mutableStateOf<String?>(null) }
    var speed by remember { mutableStateOf<String?>(null) }
    var satellites by remember { mutableStateOf("0") }
    var details by remember { mutableStateOf<List<TestDetail>>(emptyList()) }
    var permissionGranted by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Check permissions
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        permissionGranted = fineGranted || coarseGranted
        permissionDenied = !fineGranted && !coarseGranted
    }

    // Run GPS test if permission granted
    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) return@LaunchedEffect

        testStatus = TestStatus.TESTING
        viewModel.updateResult(
            TestCategory.GPS,
            TestResult(TestCategory.GPS, TestStatus.TESTING)
        )

        searchMessage = "Поиск спутников..."
        val client: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        try {
            val location: Location? = withContext(Dispatchers.IO) {
                try {
                    suspendCancellableCoroutine { cont ->
                        client.lastLocation.addOnSuccessListener { loc ->
                            cont.resume(loc)
                        }.addOnFailureListener {
                            cont.resume(null)
                        }
                    }
                } catch (e: Exception) {
                    null
                }
            }

            if (location != null) {
                latitude = String.format("%.6f°", location.latitude)
                longitude = String.format("%.6f°", location.longitude)
                accuracy = if (location.hasAccuracy()) {
                    String.format("%.1f м", location.accuracy)
                } else "N/A"
                altitude = if (location.hasAltitude()) {
                    String.format("%.1f м", location.altitude)
                } else "N/A"
                speed = if (location.hasSpeed()) {
                    String.format("%.2f м/с", location.speed)
                } else "0.00 м/с"

                // Estimate satellites from extras if available
                @Suppress("DEPRECATION")
                val satCount = location.extras?.getInt("satellites", 0) ?: 0
                if (satCount > 0) {
                    satellites = "$satCount"
                }

                details = buildList {
                    add(TestDetail("Широта", latitude!!, TestStatus.PASSED))
                    add(TestDetail("Долгота", longitude!!, TestStatus.PASSED))
                    add(TestDetail("Точность", accuracy ?: "N/A", TestStatus.PASSED))
                    add(TestDetail("Высота", altitude ?: "N/A", TestStatus.PASSED))
                    add(TestDetail("Скорость", speed ?: "N/A", TestStatus.PASSED))
                    add(
                        TestDetail(
                            "Спутники",
                            satellites,
                            if (satellites.toIntOrNull()?.let { it >= 4 } == true)
                                TestStatus.PASSED else TestStatus.SKIPPED
                        )
                    )
                }

                testStatus = TestStatus.PASSED
                viewModel.updateResult(
                    TestCategory.GPS,
                    TestResult(TestCategory.GPS, TestStatus.PASSED, details)
                )
            } else {
                // No cached location — wait up to 30 seconds for a fresh fix
                searchMessage = "Поиск спутников... (ожидание сигнала)"
                val timeoutMs = 30_000L
                val startTime = System.currentTimeMillis()
                var freshLocation: Location? = null

                while (System.currentTimeMillis() - startTime < timeoutMs && freshLocation == null) {
                    val elapsed = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    searchMessage = "Поиск спутников... ${elapsed}с / 30с"

                    try {
                        freshLocation = withContext(Dispatchers.IO) {
                            withTimeoutOrNull(5_000L) {
                                suspendCancellableCoroutine { cont ->
                                    client.getCurrentLocation(
                                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                                        null
                                    ).addOnSuccessListener { loc -> cont.resume(loc) }
                                        .addOnFailureListener { cont.resume(null) }
                                }
                            }
                        }
                    } catch (_: Exception) {
                        // Ignore and retry
                    }

                    if (freshLocation == null) {
                        delay(2_000)
                    }
                }

                if (freshLocation != null) {
                    latitude = String.format("%.6f°", freshLocation.latitude)
                    longitude = String.format("%.6f°", freshLocation.longitude)
                    accuracy = if (freshLocation.hasAccuracy()) {
                        String.format("%.1f м", freshLocation.accuracy)
                    } else "N/A"
                    altitude = if (freshLocation.hasAltitude()) {
                        String.format("%.1f м", freshLocation.altitude)
                    } else "N/A"
                    speed = if (freshLocation.hasSpeed()) {
                        String.format("%.2f м/с", freshLocation.speed)
                    } else "0.00 м/с"

                    @Suppress("DEPRECATION")
                    val satCount = freshLocation.extras?.getInt("satellites", 0) ?: 0
                    if (satCount > 0) {
                        satellites = "$satCount"
                    }

                    details = buildList {
                        add(TestDetail("Широта", latitude!!, TestStatus.PASSED))
                        add(TestDetail("Долгота", longitude!!, TestStatus.PASSED))
                        add(TestDetail("Точность", accuracy ?: "N/A", TestStatus.PASSED))
                        add(TestDetail("Высота", altitude ?: "N/A", TestStatus.PASSED))
                        add(TestDetail("Скорость", speed ?: "N/A", TestStatus.PASSED))
                        add(
                            TestDetail(
                                "Спутники",
                                satellites,
                                if (satellites.toIntOrNull()?.let { it >= 4 } == true)
                                    TestStatus.PASSED else TestStatus.SKIPPED
                            )
                        )
                    }

                    testStatus = TestStatus.PASSED
                    viewModel.updateResult(
                        TestCategory.GPS,
                        TestResult(TestCategory.GPS, TestStatus.PASSED, details)
                    )
                } else {
                    // Timeout
                    searchMessage = "Таймаут поиска спутников"
                    testStatus = TestStatus.FAILED
                    details = listOf(
                        TestDetail("Статус", "Таймаут (30с)", TestStatus.FAILED),
                        TestDetail("Причина", "Не удалось получить местоположение", TestStatus.FAILED)
                    )
                    viewModel.updateResult(
                        TestCategory.GPS,
                        TestResult(TestCategory.GPS, TestStatus.FAILED, details)
                    )
                }
            }
        } catch (e: Exception) {
            searchMessage = "Ошибка GPS"
            testStatus = TestStatus.FAILED
            details = listOf(
                TestDetail("Статус", "Ошибка", TestStatus.FAILED),
                TestDetail("Сообщение", e.localizedMessage ?: "Неизвестная ошибка", TestStatus.FAILED)
            )
            viewModel.updateResult(
                TestCategory.GPS,
                TestResult(TestCategory.GPS, TestStatus.FAILED, details)
            )
        }
    }

    TestScreenScaffold(
        title = "Тест GPS",
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Permission denied state
            if (permissionDenied) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Доступ к местоположению запрещён",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Разрешите доступ к геолокации в настройках приложения для проведения теста GPS.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.updateResult(
                        TestCategory.GPS,
                        TestResult(
                            TestCategory.GPS,
                            TestStatus.SKIPPED,
                            listOf(
                                TestDetail("Статус", "Нет разрешения", TestStatus.SKIPPED)
                            )
                        )
                    )
                }
            }

            // Searching state
            if (testStatus == TestStatus.TESTING && permissionGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.SatelliteAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = searchMessage,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = MaterialTheme.colorScheme.tertiary,
                            strokeWidth = 4.dp
                        )
                    }
                }
            }

            // Failed state
            if (testStatus == TestStatus.FAILED && permissionGranted) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = searchMessage,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Убедитесь, что GPS включён и вы находитесь на открытом воздухе.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Results
            if (details.isNotEmpty() && permissionGranted) {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Данные GPS",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
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

            // Success summary card
            if (testStatus == TestStatus.PASSED && latitude != null) {
                Spacer(modifier = Modifier.height(8.dp))
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
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📍 Местоположение определено",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$latitude, $longitude",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (accuracy != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Точность: $accuracy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}