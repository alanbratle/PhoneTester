package com.phonetester.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.phonetester.app.model.TestCategory
import com.phonetester.app.model.TestStatus
import com.phonetester.app.navigation.Screen
import com.phonetester.app.ui.components.ProgressRing
import com.phonetester.app.ui.components.TestCard
import com.phonetester.app.viewmodel.DashboardViewModel

private fun TestCategory.toScreenRoute(): String = when (this) {
    TestCategory.SCREEN -> Screen.ScreenTest.route
    TestCategory.TOUCH -> Screen.TouchTest.route
    TestCategory.BATTERY -> Screen.BatteryTest.route
    TestCategory.SENSORS -> Screen.SensorsTest.route
    TestCategory.CAMERA -> Screen.CameraTest.route
    TestCategory.AUDIO -> Screen.AudioTest.route
    TestCategory.CONNECTIVITY -> Screen.ConnectivityTest.route
    TestCategory.STORAGE -> Screen.StorageTest.route
    TestCategory.CPU -> Screen.CpuTest.route
    TestCategory.NETWORK -> Screen.NetworkTest.route
    TestCategory.DISPLAY_INFO -> Screen.DisplayInfo.route
    TestCategory.GPS -> Screen.GpsTest.route
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = viewModel(),
) {
    val results by viewModel.results.collectAsStateWithLifecycle()

    val passedCount by remember {
        derivedStateOf { results.values.count { it.status == TestStatus.PASSED } }
    }
    val failedCount by remember {
        derivedStateOf { results.values.count { it.status == TestStatus.FAILED } }
    }
    val skippedCount by remember {
        derivedStateOf {
            results.values.count {
                it.status == TestStatus.SKIPPED || it.status == TestStatus.PENDING
            }
        }
    }
    val totalTests = viewModel.totalTests
    val progress = viewModel.progress

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { navController.navigate(Screen.TestResult.route) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Результаты", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { navController.navigate(Screen.About.route) },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("О приложении", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            item(span = { GridItemSpan(2) }) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Phone Tester Pro",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Комплексная диагностика смартфона",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Progress Ring
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ProgressRing(
                        progress = progress,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Stats Row
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatChip(label = "Пройдено", count = passedCount, color = Color(0xFF4CAF50))
                    StatChip(label = "Не пройдено", count = failedCount, color = Color(0xFFF44336))
                    StatChip(label = "Пропущено", count = skippedCount, color = Color(0xFF9E9E9E))
                }
            }

            // Run All Tests Button
            item(span = { GridItemSpan(2) }) {
                Button(
                    onClick = {
                        // Navigate to the first pending test
                        val nextPending = TestCategory.all.firstOrNull {
                            results[it]?.status == TestStatus.PENDING
                        }
                        if (nextPending != null) {
                            navController.navigate(nextPending.toScreenRoute())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        "Запустить все тесты",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Test Cards
            items(TestCategory.all, key = { it.name }) { category ->
                val result = results[category]
                TestCard(
                    category = category,
                    status = result?.status ?: TestStatus.PENDING,
                    onClick = { navController.navigate(category.toScreenRoute()) }
                )
            }

            // Bottom spacing
            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun StatChip(label: String, count: Int, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}