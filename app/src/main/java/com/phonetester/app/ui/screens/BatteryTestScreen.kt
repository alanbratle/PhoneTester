package com.phonetester.app.ui.screens

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phonetester.app.ui.components.TestScreenScaffold
import kotlinx.coroutines.delay

data class BatteryInfoData(
    val level: Int,
    val temperature: Float,
    val voltage: Int,
    val isCharging: Boolean,
    val health: String,
    val technology: String,
    val chargingType: String,
    val status: String
)

fun getBatteryInfoData(context: Context): BatteryInfoData {
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    val bs = context.registerReceiver(null, filter)

    val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    val temp = ((bs?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f)
    val volt = bs?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
    val st = bs?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val hp = bs?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
    val plug = bs?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1
    val tech = bs?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

    val charging = st == BatteryManager.BATTERY_STATUS_CHARGING ||
            st == BatteryManager.BATTERY_STATUS_FULL

    val chargeType = when (plug) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> "Not Charging"
    }

    val statusText = when (st) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }

    val healthText = when (hp) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        else -> "Unknown"
    }

    return BatteryInfoData(level, temp, volt, charging, healthText, tech, chargeType, statusText)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryTestScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var info by remember { mutableStateOf(getBatteryInfoData(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            info = getBatteryInfoData(context)
        }
    }

    TestScreenScaffold(title = "Battery Test", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Battery Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "${info.level}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (info.isCharging) "Charging via ${info.chargingType}" else "Discharging",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Battery Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    BatteryDetailRow("Status", info.status)
                    BatteryDetailRow("Health", info.health)
                    BatteryDetailRow("Technology", info.technology)
                    BatteryDetailRow("Temperature", "${info.temperature} C")
                    BatteryDetailRow("Voltage", "${info.voltage} mV")
                    BatteryDetailRow("Power Source", info.chargingType)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (info.level > 0 && info.health == "Good")
                        MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Test Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (info.level > 0 && info.health == "Good")
                            "Battery is functioning normally"
                        else if (info.health != "Good")
                            "Battery health issue detected: ${info.health}"
                        else
                            "Battery level too low for accurate testing",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun BatteryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}