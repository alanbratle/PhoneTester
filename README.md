# 📱 Phone Tester Pro

<p align="center">
  <strong>Комплексная диагностика Android-смартфона</strong><br>
  Полное тестирование всех аппаратных компонентов устройства
</p>

---

## 🧪 Тесты (12 модулей)

| Модуль | Описание |
|--------|----------|
| 📱 **Экран** | Цветовая палитра, битые пиксели, градиенты |
| 👆 **Тачскрин** | Мультитач, отклик, количество одновременных точек |
| 🔋 **Батарея** | Здоровье, температура, напряжение, статус зарядки |
| 📡 **Сенсоры** | Акселерометр, гироскоп, магнитометр, датчик света и др. |
| 📷 **Камера** | Предварительный просмотр передней и задней камеры |
| 🔊 **Аудио** | Тест динамика (ToneGenerator) + запись/воспроизведение микрофона |
| 📶 **Связь** | Wi-Fi (SSID, RSSI, IP), Bluetooth, сотовая связь (2G-5G) |
| 💾 **Хранилище** | Объём, скорость чтения/записи (10 МБ бенчмарк) |
| ⚡ **Процессор** | Ядра, частота, архитектура, бенчмарк (решето Эратосфена) |
| 🌐 **Сеть** | Пинг, скорость скачивания/загрузки, тип подключения |
| 🖥️ **Дисплей** | Разрешение, DPI, частота обновления, HDR, размер |
| 📍 **GPS** | Координаты, точность, высота, скорость, спутники |

## 🛠 Технологии

- **Kotlin** — основной язык
- **Jetpack Compose** + **Material Design 3** — UI
- **MVVM** — архитектура (ViewModel + StateFlow)
- **CameraX** — работа с камерой
- **Google Play Services Location** — GPS
- **Navigation Compose** — навигация
- **Version Catalog** (libs.versions.toml) — управление зависимостями
- **minSdk 26** (Android 8.0) / **targetSdk 34** (Android 14)

## 📦 Структура проекта

```
app/src/main/java/com/phonetester/app/
├── MainActivity.kt              # Точка входа + навигация
├── PhoneTesterApp.kt            # Application class
├── model/
│   └── TestModels.kt            # Data-классы и enum
├── navigation/
│   └── Screen.kt                # Маршруты навигации
├── ui/
│   ├── theme/                   # Color, Type, Theme, Shape
│   ├── components/              # TestCard, ProgressRing, DetailRow
│   └── screens/                 # 14 экранов (Dashboard + 12 тестов + Results + About)
└── viewmodel/
    ├── DashboardViewModel.kt    # Статус всех тестов
    └── TestResultViewModel.kt   # Информация об устройстве
```

## 🚀 Сборка

```bash
# Клонировать
git clone https://github.com/YOUR_USERNAME/PhoneTester.git
cd PhoneTester

# Открыть в Android Studio и собрать
# Или через командную строку:
./gradlew assembleDebug
```

## 📋 Требования

- Android Studio Hedgehog (2023.1.1) или новее
- Android SDK 34
- JDK 17

## 📄 Лицензия

MIT License — см. файл [LICENSE](LICENSE)