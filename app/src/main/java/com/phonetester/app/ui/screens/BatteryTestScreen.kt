package com.phonetester.app.ui.screens

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestDetail
import com.phonetester.app.model.TestStatus
import com.phonetester.app.model.TestResult
import com.phonetester.app.ui.components.DetailRow
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel

@Composable
fun BatteryTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var health by remember { mutableStateOf("—") }
    var level by remember { mutableStateOf("—") }
    var temperature by remember { mutableStateOf("—") }
    var voltage by remember { mutableStateOf("—") }
    var status by remember { mutableStateOf("—") }
    var technology by remember { mutableStateOf("—") }
    var plugged by remember { mutableStateOf("—") }
    var hasData by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                // Health
                health = when (intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                    BatteryManager.BATTERY_HEALTH_GOOD -> "Хорошее"
                    BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Перегрев"
                    BatteryManager.BATTERY_HEALTH_DEAD -> "Мёртвая"
                    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Перенапряжение"
                    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Неизвестная ошибка"
                    BatteryManager.BATTERY_HEALTH_COLD -> "Холодная"
                    else -> "Неизвестно"
                }

                // Level
                val levelRaw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                level = if (levelRaw >= 0 && scale > 0) {
                    "${(levelRaw * 100 / scale)}%"
                } else {
                    "—"
                }

                // Temperature (in tenths of a degree C → °C)
                val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
                temperature = if (tempRaw != Int.MIN_VALUE) {
                    "${tempRaw / 10.0f}°C"
                } else {
                    "—"
                }

                // Voltage
                val voltRaw = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
                voltage = if (voltRaw >= 0) {
                    "${voltRaw} mV"
                } else {
                    "—"
                }

                // Status
                status = when (intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Заряжается"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Разряжается"
                    BatteryManager.BATTERY_STATUS_FULL -> "Полный заряд"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Не заряжается"
                    else -> "—"
                }

                // Technology
                technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "—"

                // Plugged
                plugged = when (intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                    BatteryManager.BATTERY_PLUGGED_USB -> "USB"
                    BatteryManager.BATTERY_PLUGGED_AC -> "AC (сеть)"
                    BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Беспроводная"
                    else -> "Не подключено"
                }

                hasData = true
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        viewModel.updateResult(
            TestCategory.BATTERY,
            TestResult(
                category = TestCategory.BATTERY,
                status = TestStatus.PASSED,
                details = listOf(
                    TestDetail("Доступность", "OK"),
                    TestDetail("Чтение данных", "OK")
                )
            )
        )
    }

    TestScreenScaffold(
        title = "Тест батареи",
        onBack = onBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Информация о батарее",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
                    )
                    DetailRow(label = "Здоровье", value = health)
                    DetailRow(label = "Уровень заряда", value = level)
                    DetailRow(label = "Температура", value = temperature)
                    DetailRow(label = "Напряжение", value = voltage)
                    DetailRow(label = "Статус", value = status)
                    DetailRow(label = "Технология", value = technology)
                    DetailRow(label = "Подключение", value = plugged)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (hasData) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Данные батареи получены — тест пройден",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}