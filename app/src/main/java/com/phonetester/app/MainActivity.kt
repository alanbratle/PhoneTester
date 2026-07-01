package com.phonetester.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.phonetester.app.navigation.Screen
import com.phonetester.app.ui.screens.AboutScreen
import com.phonetester.app.ui.screens.AudioTestScreen
import com.phonetester.app.ui.screens.BatteryTestScreen
import com.phonetester.app.ui.screens.CameraTestScreen
import com.phonetester.app.ui.screens.ConnectivityTestScreen
import com.phonetester.app.ui.screens.CpuTestScreen
import com.phonetester.app.ui.screens.DashboardScreen
import com.phonetester.app.ui.screens.DisplayInfoScreen
import com.phonetester.app.ui.screens.GpsTestScreen
import com.phonetester.app.ui.screens.NetworkTestScreen
import com.phonetester.app.ui.screens.SensorsTestScreen
import com.phonetester.app.ui.screens.ScreenTestScreen
import com.phonetester.app.ui.screens.StorageTestScreen
import com.phonetester.app.ui.screens.TestResultScreen
import com.phonetester.app.ui.screens.TouchTestScreen
import com.phonetester.app.ui.theme.PhoneTesterTheme
import com.phonetester.app.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhoneTesterTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation(
    navController: NavHostController = rememberNavController(),
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val results by dashboardViewModel.results.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Auto-advance: when a test is completed, pop back to dashboard
    LaunchedEffect(Unit) {
        // No-op; individual screens handle their own navigation
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        // Dashboard
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController, dashboardViewModel)
        }

        // Test Result Summary
        composable(Screen.TestResult.route) {
            TestResultScreen(
                context = context,
                dashboardViewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // About
        composable(Screen.About.route) {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        // === Individual Test Screens ===

        composable(Screen.ScreenTest.route) {
            ScreenTestScreen(
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.TouchTest.route) {
            TouchTestScreen(
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BatteryTest.route) {
            BatteryTestScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.SensorsTest.route) {
            SensorsTestScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CameraTest.route) {
            CameraTestScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.AudioTest.route) {
            AudioTestScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ConnectivityTest.route) {
            ConnectivityTestScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.StorageTest.route) {
            StorageTestScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CpuTest.route) {
            CpuTestScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.NetworkTest.route) {
            NetworkTestScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.DisplayInfo.route) {
            DisplayInfoScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.GpsTest.route) {
            GpsTestScreen(
                context = context,
                viewModel = dashboardViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}