# Smart Watering

Kotlin/Jetpack Compose MVP for the Smart Watering coursework project.

## Architecture

The app is organized by responsibility so screens stay focused and data access is kept out of UI code:

- `ui/App.kt` owns the navigation graph, bottom navigation, and app-level theme state.
- `ui/screens/` contains one Compose screen per file: Today, Plants, Add Plant, Plant Detail, and Settings.
- `ui/components/` contains reusable garden and settings UI elements.
- `ui/state/SmartWateringViewModel.kt` owns screen state and user actions; composables observe its `StateFlow`.
- `data/PlantRepository.kt` is the app-facing data boundary. It wraps Room and keeps plant deletion transactional.
- `data/Database.kt` contains Room entities, converters, DAO queries, and database creation.
- `model/` contains species, weather data, feature construction, decision-tree prediction, and explanations.
- `weather/WeatherClient.kt` fetches/caches Open-Meteo data and maps daily weather fields into the model.
- `work/` schedules and runs daily predictions. The worker uses the repository instead of accessing Room from UI code.
- `ui/util/` contains date formatting and CSV export helpers.

The app uses Alfred's schema v1 decision tree with the original 12-feature order. The Android suite keeps the 30 original reference cases and adds 508 synthetic cases covering every leaf and the adjacent float32 values on each split. The synthetic cases verify Android's traversal.

For a revised export, first verify the notebook's reference cases. Then generate the structural probes with `python3 scripts/expand_model_parity.py decision_tree.json android_parity_cases.json` and save its output as the app's parity fixture.

## Run the app

1. Open the project folder in Android Studio and install Android SDK Platform 35.
2. Use the Gradle wrapper and a compatible Gradle JDK, then sync the project.
3. Run the `app` configuration on an emulator or an Android 8.0+ device.
4. Add a plant, select its last watering date, and open its care history to log observations.
5. In Settings, grant notification access for the morning reminder. Care runs around 7:00 a.m. Nairobi time, subject to Android background scheduling.
6. Refresh once while online to cache the day's weather. Settings also exports care history as CSV with Nairobi timestamps.

Run `./gradlew :app:testDebugUnitTest :app:assembleDebug` to check model parity and build a debug APK.

When Alfred provides a revised model export, replace:

- `app/src/main/assets/decision_tree.json`
- `app/src/test/resources/decision_tree.json`
- `app/src/test/resources/android_parity_cases.json`

Then run `TreeParityTest` from Android Studio before the demo.
