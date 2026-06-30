package com.phonetester.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
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
fun ConnectivityTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var wifiStatus by remember { mutableStateOf("---") }
    var wifiSSID by remember { mutableStateOf("---") }
    var wifiRSSI by remember { mutableStateOf("---") }
    var btStatus by remember { mutableStateOf("---") }
    var cellStatus by remember { mutableStateOf("---") }
    var cellType by remember { mutableStateOf("---") }
    var cellOp by remember { mutableStateOf("---") }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)

        wifiStatus = if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true) "Connected" else "Disconnected"
        cellStatus = if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true) "Connected" else "Disconnected"

        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wm.connectionInfo
            wifiSSID = info?.ssid?.removeSurrounding("\"") ?: "---"
            @Suppress("DEPRECATION")
            wifiRSSI = "${info?.rssi ?: 0} dBm"
        } catch (_: Exception) {}

        try {
            btStatus = if (android.bluetooth.BluetoothAdapter.getDefaultAdapter()?.isEnabled == true) "Enabled" else "Disabled"
        } catch (_: Exception) { btStatus = "N/A" }

        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? android.telephony.TelephonyManager
            cellOp = tm?.networkOperatorName ?: "---"
            @Suppress("DEPRECATION")
            cellType = when (tm?.networkType ?: -1) {
                android.telephony.TelephonyManager.NETWORK_TYPE_LTE -> "LTE/4G"
                android.telephony.TelephonyManager.NETWORK_TYPE_NR -> "5G"
                in 13..15 -> "3G"
                in 1..4 -> "2G"
                else -> "Unknown"
            }
        } catch (_: Exception) {}

        val details = listOf(
            TestDetail("Wi-Fi", wifiStatus, if (wifiStatus == "Connected") TestStatus.PASSED else TestStatus.FAILED),
            TestDetail("SSID", wifiSSID),
            TestDetail("Wi-Fi RSSI", wifiRSSI),
            TestDetail("Bluetooth", btStatus, if (btStatus == "Enabled") TestStatus.PASSED else TestStatus.FAILED),
            TestDetail("Cellular", cellStatus, if (cellStatus == "Connected") TestStatus.PASSED else TestStatus.FAILED),
            TestDetail("Network", cellType),
            TestDetail("Operator", cellOp)
        )
        val status = if (wifiStatus == "Connected" || cellStatus == "Connected") TestStatus.PASSED else TestStatus.FAILED
        viewModel.updateResult(TestCategory.CONNECTIVITY, TestResult(TestCategory.CONNECTIVITY, status, details))
        done = true
    }

    TestScreenScaffold(title = "Connectivity Test", onBack = onBack) { pv ->
        Column(modifier = Modifier.padding(pv).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Wi-Fi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DetailRow("Status", wifiStatus)
            DetailRow("SSID", wifiSSID)
            DetailRow("Signal", wifiRSSI)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Bluetooth", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DetailRow("Status", btStatus)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Cellular", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DetailRow("Status", cellStatus)
            DetailRow("Type", cellType)
            DetailRow("Operator", cellOp)
            if (done) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            }
        }
    }
}