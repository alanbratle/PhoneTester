package com.phonetester.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import android.bluetooth.BluetoothManager
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestDetail
import com.phonetester.app.model.TestResult
import com.phonetester.app.model.TestStatus
import com.phonetester.app.ui.components.DetailRow
import com.phonetester.app.ui.components.TestScreenScaffold
import com.phonetester.app.viewmodel.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ConnectivityTestScreen(
    context: Context,
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    var wifiConnected by remember { mutableStateOf(false) }
    var wifiSSID by remember { mutableStateOf("N/A") }
    var wifiRSSI by remember { mutableStateOf(0) }
    var wifiIPAddress by remember { mutableStateOf("N/A") }
    var wifiLinkSpeed by remember { mutableStateOf(0) }
    var bluetoothEnabled by remember { mutableStateOf(false) }
    var cellularConnected by remember { mutableStateOf(false) }
    var cellularNetworkType by remember { mutableStateOf("N/A") }
    var cellularOperator by remember { mutableStateOf("N/A") }
    var details by remember { mutableStateOf<List<TestDetail>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val d = mutableListOf<TestDetail>()

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiInfo = try {
                @Suppress("DEPRECATION")
                wifiManager.connectionInfo
            } catch (_: Exception) { null }
            val network = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(network)
            val hasWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isWifi = hasWifi && wifiInfo != null && wifiInfo.networkId != -1
            wifiConnected = isWifi

            if (isWifi && wifiInfo != null) {
                @Suppress("DEPRECATION")
                wifiSSID = wifiInfo.ssid?.removeSurrounding("\"") ?: "N/A"
                @Suppress("DEPRECATION")
                wifiRSSI = wifiInfo.rssi
                @Suppress("DEPRECATION")
                val ip = wifiInfo.ipAddress
                wifiIPAddress = String.format("%d.%d.%d.%d", ip and 0xFF, (ip shr 8) and 0xFF, (ip shr 16) and 0xFF, (ip shr 24) and 0xFF)
                @Suppress("DEPRECATION")
                wifiLinkSpeed = wifiInfo.linkSpeed
                d.add(TestDetail("Wi-Fi", "Connected - $wifiSSID", TestStatus.PASSED))
            } else {
                d.add(TestDetail("Wi-Fi", "Not connected", TestStatus.SKIPPED))
            }

            val hasBtPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            if (hasBtPerm) {
                val bm = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                bluetoothEnabled = bm?.adapter?.isEnabled == true
            }
            d.add(TestDetail("Bluetooth", if (bluetoothEnabled) "Enabled" else "Disabled", if (bluetoothEnabled) TestStatus.PASSED else TestStatus.SKIPPED))

            val tm = context.applicationContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            cellularConnected = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
            if (cellularConnected) {
                @Suppress("DEPRECATION")
                cellularNetworkType = when (tm.networkType) {
                    TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN -> "LTE/4G"
                    TelephonyManager.NETWORK_TYPE_NR -> "5G"
                    else -> "Other"
                }
                cellularOperator = tm.networkOperatorName ?: "Unknown"
                d.add(TestDetail("Cellular", "$cellularNetworkType ($cellularOperator)", TestStatus.PASSED))
            } else {
                d.add(TestDetail("Cellular", "Not connected", TestStatus.SKIPPED))
            }

            details = d
            viewModel.updateResult(
                TestCategory.CONNECTIVITY,
                TestResult(TestCategory.CONNECTIVITY, TestStatus.PASSED, details)
            )
        }
    }

    TestScreenScaffold(title = "Connectivity Test", onBack = onBack) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Wi-Fi", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DetailRow(label = "Status", value = if (wifiConnected) "Connected" else "Disconnected")
            if (wifiConnected) {
                DetailRow(label = "SSID", value = wifiSSID)
                DetailRow(label = "Signal", value = "$wifiRSSI dBm")
                DetailRow(label = "IP Address", value = wifiIPAddress)
                DetailRow(label = "Link Speed", value = "$wifiLinkSpeed Mbps")
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Bluetooth", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DetailRow(label = "Status", value = if (bluetoothEnabled) "Enabled" else "Disabled")

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Cellular", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            DetailRow(label = "Status", value = if (cellularConnected) "Connected" else "Disconnected")
            if (cellularConnected) {
                DetailRow(label = "Network Type", value = cellularNetworkType)
                DetailRow(label = "Operator", value = cellularOperator)
            }
        }
    }
}