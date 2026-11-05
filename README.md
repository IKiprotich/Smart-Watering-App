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

Temporary dummy `decision_tree.json` and `android_parity_cases.json` files are included so the app can run while Alfred prepares the real export. They use one simple rule: water after more than one rain-free day. Replace both copies with Alfred's files when they arrive.

## Run

Open this folder in Android Studio, allow Gradle sync, and run the `app` configuration on an API 34+ emulator or Android phone.

## Run it

1. Install the latest stable Android Studio.
2. Open `/Users/ian/Documents/ChatGPT/mobile app`.
3. Open `Android Studio > Settings > Build, Execution, Deployment > Build Tools > Gradle`.
4. Set `Gradle JDK` to `Embedded JDK` or another JDK 17 installation.
5. Open `Tools > SDK Manager` and install `Android 15 (API 35)` including `Android SDK Platform 35`.
6. Click `Sync Project with Gradle Files`.
7. Create/start a Pixel emulator using API 35 or newer.
8. Press Run for the `app` configuration.
9. Add a plant. The app saves it locally and starts a decision refresh.
10. If weather is unavailable, run once while online first so the cache exists.

When Alfred sends the real model, replace:

- `app/src/main/assets/decision_tree.json`
- `app/src/test/resources/decision_tree.json`
- `app/src/test/resources/android_parity_cases.json`

Then run `TreeParityTest` from Android Studio before the demo.
