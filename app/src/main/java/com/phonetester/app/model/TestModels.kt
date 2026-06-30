package com.phonetester.app.model

enum class TestStatus {
    PENDING, TESTING, PASSED, FAILED, SKIPPED
}

enum class TestCategory(
    val key: String,
    val displayName: String,
    val description: String,
    val icon: String
) {
    SCREEN("screen", "Экран", "Цвета и битые пиксели", "smartphone"),
    TOUCH("touch", "Тачскрин", "Мультитач и отклик", "touch_app"),
    BATTERY("battery", "Батарея", "Здоровье и зарядка", "battery_full"),
    SENSORS("sensors", "Сенсоры", "Датчики устройства", "sensors"),
    CAMERA("camera", "Камера", "Передняя и задняя", "photo_camera"),
    AUDIO("audio", "Аудио", "Динамик и микрофон", "volume_up"),
    CONNECTIVITY("connectivity", "Связь", "Wi-Fi, BT, сотовая", "wifi"),
    STORAGE("storage", "Хранилище", "Скорость чтения/записи", "storage"),
    CPU("cpu", "Процессор", "Производительность CPU", "memory"),
    NETWORK("network", "Сеть", "Скорость интернета", "speed"),
    DISPLAY_INFO("display_info", "Дисплей", "Характеристики экрана", "monitor"),
    GPS("gps", "GPS", "Местоположение", "location_on");

    companion object {
        val all = entries
    }
}

data class TestResult(
    val category: TestCategory,
    val status: TestStatus = TestStatus.PENDING,
    val details: List<TestDetail> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class TestDetail(
    val label: String,
    val value: String,
    val status: TestStatus = TestStatus.PASSED
)

data class DeviceInfo(
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val sdkVersion: Int,
    val totalRam: Long,
    val cpuCores: Int,
    val cpuAbi: String
)