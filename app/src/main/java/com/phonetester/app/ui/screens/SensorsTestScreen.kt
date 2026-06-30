package com.phonetester.app.ui.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestDetail
import com.phonetester.app.model.TestStatus
import com.phonetester.app.model.TestResult
import com.phonetester.app.ui.components.DetailRow
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel

private data class SensorEntry(
    val name: String,
    val type: Int,
    val available: Boolean,
    val liveValues: String
)

@Composable
fun SensorsTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    val sensorTypes = remember {
        listOf(
            "Акселерометр" to Sensor.TYPE_ACCELEROMETER,
            "Гироскоп" to Sensor.TYPE_GYROSCOPE,
            "Магнитометр" to Sensor.TYPE_MAGNETIC_FIELD,
            "Датчик приближения" to Sensor.TYPE_PROXIMITY,
            "Датчик освещённости" to Sensor.TYPE_LIGHT,
            "Счётчик шагов" to Sensor.TYPE_STEP_COUNTER,
            "Вектор вращения" to Sensor.TYPE_ROTATION_VECTOR
        )
    }

    // Mutable map tracking live values per sensor type
    val sensorValues = remember { mutableStateMapOf<Int, String>() }

    // Build the sensor entries list — recomputes every time sensorValues contents change
    val sensorEntries by remember {
        derivedStateOf {
            sensorTypes.map { (name, type) ->
                val sensor: Sensor? = sensorManager.getDefaultSensor(type)
                val available = sensor != null
                SensorEntry(
                    name = name,
                    type = type,
                    available = available,
                    liveValues = if (available) sensorValues[type] ?: "Ожидание данных…" else "Недоступен"
                )
            }
        }
    }

    // Register listeners for all available sensors
    LaunchedEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val type = event.sensor.type
                val values = event.values.joinToString(", ") { "%.2f".format(it) }
                sensorValues[type] = values
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // No action needed
            }
        }

        // Register all available sensors at normal rate
        val registeredSensors = mutableListOf<Sensor>()
        for ((_, type) in sensorTypes) {
            val sensor = sensorManager.getDefaultSensor(type)
            if (sensor != null) {
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                registeredSensors.add(sensor)
            }
        }

        // Auto-pass: count available sensors and report
        val availableCount = sensorTypes.count { (_, type) ->
            sensorManager.getDefaultSensor(type) != null
        }
        val details = sensorTypes.map { (name, type) ->
            val available = sensorManager.getDefaultSensor(type) != null
            TestDetail(
                label = name,
                value = if (available) "Доступен" else "Не найден",
                status = if (available) TestStatus.PASSED else TestStatus.FAILED
            )
        }
        viewModel.updateResult(
            TestCategory.SENSORS,
            TestResult(
                category = TestCategory.SENSORS,
                status = if (availableCount > 0) TestStatus.PASSED else TestStatus.FAILED,
                details = details
            )
        )

        // Keep the coroutine alive so listeners stay registered;
        // finally block runs when the composable leaves composition
        try {
            kotlinx.coroutines.delay(Long.MAX_VALUE)
        } finally {
            sensorManager.unregisterListener(listener)
        }
    }

    val availableCount = sensorEntries.count { it.available }

    TestScreenScaffold(
        title = "Тест сенсоров",
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (availableCount > 0) Icons.Default.CheckCircle else Icons.Default.Close,
                        contentDescription = null,
                        tint = if (availableCount > 0) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Найдено сенсоров: $availableCount из ${sensorEntries.size}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sensor list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = sensorEntries,
                    key = { it.type }
                ) { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (entry.available)
                                MaterialTheme.colorScheme.surface
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Header: name + status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = entry.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = if (entry.available)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = if (entry.available) "Доступен" else "Нет",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (entry.available)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            // Live values
                            if (entry.available) {
                                DetailRow(
                                    label = "Данные",
                                    value = entry.liveValues,
                                    status = if (entry.liveValues == "Ожидание данных…")
                                        TestStatus.TESTING
                                    else
                                        TestStatus.PASSED
                                )
                            } else {
                                Text(
                                    text = "Сенсор не найден на устройстве",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}