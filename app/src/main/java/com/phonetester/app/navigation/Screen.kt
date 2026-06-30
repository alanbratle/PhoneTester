package com.phonetester.app.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object TestResult : Screen("test_result")
    data object About : Screen("about")

    // Individual test screens
    data object ScreenTest : Screen("test_screen")
    data object TouchTest : Screen("test_touch")
    data object BatteryTest : Screen("test_battery")
    data object SensorsTest : Screen("test_sensors")
    data object CameraTest : Screen("test_camera")
    data object AudioTest : Screen("test_audio")
    data object ConnectivityTest : Screen("test_connectivity")
    data object StorageTest : Screen("test_storage")
    data object CpuTest : Screen("test_cpu")
    data object NetworkTest : Screen("test_network")
    data object DisplayInfo : Screen("test_display_info")
    data object GpsTest : Screen("test_gps")
}