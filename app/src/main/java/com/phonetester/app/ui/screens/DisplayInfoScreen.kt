package com.phonetester.app.ui.screens

import android.content.Context
import android.os.Build
import android.view.WindowManager
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phonetester.app.ui.components.TestScreenScaffold

data class DisplayInfoData(
    val resolution: String,
    val densityDpi: Int,
    val density: Float,
    val refreshRate: Float,
    val scaledDensity: Float,
    val colorMode: String
)

fun getDisplayInfoData(context: Context): DisplayInfoData {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    @Suppress("DEPRECATION")
    val display = wm.defaultDisplay
    val m = context.resources.displayMetrics

    val colorModeStr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val mode = display.colorMode
        when (mode) {
            0 -> "Default (sRGB)"
            1 -> "Wide Color Gamut"
            else -> "Mode $mode"
        }
    } else {
        "Default (sRGB) [API < 26]"
    }

    return DisplayInfoData(
        resolution = "${m.widthPixels} x ${m.heightPixels} px",
        densityDpi = m.densityDpi,
        density = m.density,
        refreshRate = display.refreshRate,
        scaledDensity = m.scaledDensity,
        colorMode = colorModeStr
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val info = remember { getDisplayInfoData(context) }

    TestScreenScaffold(title = "Display Info", onBack = onBack) {
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
                        "Screen Resolution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        info.resolution,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
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
                        "Display Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    DisplayDetailRow("Density (dpi)", "${info.densityDpi}")
                    DisplayDetailRow("Density", "${info.density}")
                    DisplayDetailRow("Scaled Density", "${info.scaledDensity}")
                    DisplayDetailRow("Refresh Rate", "${info.refreshRate} Hz")
                    DisplayDetailRow("Color Mode", info.colorMode)
                }
            }
        }
    }
}

@Composable
private fun DisplayDetailRow(label: String, value: String) {
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